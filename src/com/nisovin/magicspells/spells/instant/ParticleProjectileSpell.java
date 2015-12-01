package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ParticleProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	float startYOffset;
	float startForwardOffset;
	
	float projectileVelocity;
	float projectileVelocityVertOffset;
	float projectileVelocityHorizOffset;
	float projectileGravity;
	float projectileSpread;
	boolean powerAffectsVelocity;
	
	int tickInterval;
	float ticksPerSecond;
	int specialEffectInterval;
	int spellInterval;
	
	String particleName;
	float particleSpeed;
	int particleCount;
	float particleXSpread;
	float particleYSpread;
	float particleZSpread;
	
	int maxDistanceSquared;
	int maxDuration;
	float hitRadius;
	float verticalHitRadius;
	int renderDistance;
	
	boolean hugSurface;
	float heightFromSurface;
	
	boolean hitPlayers;
	boolean hitNonPlayers;
	boolean hitSelf;
	boolean hitGround;
	boolean hitAirAtEnd;
	boolean hitAirAfterDuration;
	boolean hitAirDuring;
	boolean stopOnHitEntity;
	boolean stopOnHitGround;
	
	String landSpellName;
	Subspell spell;
	
	ParticleProjectileSpell thisSpell;
	Random rand = new Random();

	public ParticleProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		thisSpell = this;
		
		startYOffset = getConfigFloat("start-y-offset", 1F);
		startForwardOffset = getConfigFloat("start-forward-offset", 1F);
		
		projectileVelocity = getConfigFloat("projectile-velocity", 10F);
		projectileVelocityVertOffset = getConfigFloat("projectile-vert-offset", 0F);
		projectileVelocityHorizOffset = getConfigFloat("projectile-horiz-offset", 0F);
		projectileGravity = getConfigFloat("projectile-gravity", 0.25F);
		projectileSpread = getConfigFloat("projectile-spread", 0F);
		powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);
		
		tickInterval = getConfigInt("tick-interval", 2);
		ticksPerSecond = 20F / (float)tickInterval;
		specialEffectInterval = getConfigInt("special-effect-interval", 0);
		spellInterval = getConfigInt("spell-interval", 20);
		
		particleName = getConfigString("particle-name", "reddust");
		particleSpeed = getConfigFloat("particle-speed", 0.3F);
		particleCount = getConfigInt("particle-count", 15);
		particleXSpread = getConfigFloat("particle-horizontal-spread", 0.3F);
		particleYSpread = getConfigFloat("particle-vertical-spread", 0.3F);
		particleZSpread = particleXSpread;
		particleXSpread = getConfigFloat("particle-red", particleXSpread);
		particleYSpread = getConfigFloat("particle-green", particleYSpread);
		particleZSpread = getConfigFloat("particle-blue", particleZSpread);
		
		maxDistanceSquared = getConfigInt("max-distance", 15);
		maxDistanceSquared *= maxDistanceSquared;
		maxDuration = getConfigInt("max-duration", 0) * 1000;
		hitRadius = getConfigFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", hitRadius);
		renderDistance = getConfigInt("render-distance", 32);
		
		hugSurface = getConfigBoolean("hug-surface", false);
		if (hugSurface) {
			heightFromSurface = getConfigFloat("height-from-surface", .6F);
		} else {
			heightFromSurface = 0;
		}
		
		hitPlayers = getConfigBoolean("hit-players", false);
		hitNonPlayers = getConfigBoolean("hit-non-players", true);
		hitSelf = getConfigBoolean("hit-self", false);
		hitGround = getConfigBoolean("hit-ground", true);
		hitAirAtEnd = getConfigBoolean("hit-air-at-end", false);
		hitAirAfterDuration = getConfigBoolean("hit-air-after-duration", false);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		
		landSpellName = getConfigString("spell", "explode");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		Subspell s = new Subspell(landSpellName);
		if (s.process()) {
			spell = s;
		} else {
			MagicSpells.error("ParticleProjectileSpell " + internalName + " has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new ProjectileTracker(player, player.getLocation(), power);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	class ProjectileTracker implements Runnable {
		
		Player caster;
		float power;
		long startTime;
		Location startLocation;
		Location previousLocation;
		Location currentLocation;
		Vector currentVelocity;
		int currentX;
		int currentZ;
		int taskId;
		List<LivingEntity> inRange;
		Map<LivingEntity, Long> immune;
		
		int counter = 0;
		
		public ProjectileTracker(Player caster, Location from, float power) {
			this.caster = caster;
			this.power = power;
			this.startTime = System.currentTimeMillis();
			this.startLocation = from.clone();
			if (startYOffset != 0) {
				this.startLocation.setY(this.startLocation.getY() + startYOffset);
			}
			if (startForwardOffset != 0) {
				this.startLocation.add(this.startLocation.getDirection().clone().multiply(startForwardOffset));
			}
			this.previousLocation = startLocation.clone();
			this.currentLocation = startLocation.clone();
			this.currentVelocity = from.getDirection();
			if (projectileVelocityHorizOffset != 0) {
				Util.rotateVector(this.currentVelocity, projectileVelocityHorizOffset);
			}
			if (projectileVelocityVertOffset != 0) {
				this.currentVelocity.add(new Vector(0, projectileVelocityVertOffset, 0)).normalize();
			}
			if (projectileSpread > 0) {
				this.currentVelocity.add(new Vector(rand.nextFloat() * projectileSpread, rand.nextFloat() * projectileSpread, rand.nextFloat() * projectileSpread));
			}
			if (hugSurface) {
				this.currentLocation.setY((int)this.currentLocation.getY() + heightFromSurface);
				this.currentVelocity.setY(0).normalize();
			}
			if (powerAffectsVelocity) {
				this.currentVelocity.multiply(power);
			}
			this.currentVelocity.multiply(projectileVelocity / ticksPerSecond);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			if (hitPlayers || hitNonPlayers) {
				this.inRange = currentLocation.getWorld().getLivingEntities();
				Iterator<LivingEntity> iter = inRange.iterator();
				while (iter.hasNext()) {
					LivingEntity e = iter.next();
					if (!hitSelf && caster != null && e.equals(caster)) {
						iter.remove();
						continue;
					}
					if (!hitPlayers && e instanceof Player) {
						iter.remove();
						continue;
					}
					if (!hitNonPlayers && !(e instanceof Player)) {
						iter.remove();
						continue;
					}
				}
			}
			this.immune = new HashMap<LivingEntity, Long>();
		}
		
		@Override
		public void run() {
			if (caster != null && !caster.isValid()) {
				stop();
				return;
			}
			
			// check if duration is up
			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (hitAirAfterDuration && spell != null && spell.isTargetedLocationSpell()) {
					spell.castAtLocation(caster, currentLocation, power);
					playSpellEffects(EffectPosition.TARGET, currentLocation);
				}
				stop();
				return;
			}
			
			// move projectile and apply gravity
			previousLocation = currentLocation.clone();
			currentLocation.add(currentVelocity);
			if (hugSurface) {
				if (currentLocation.getBlockX() != currentX || currentLocation.getBlockZ() != currentZ) {
					Block b = currentLocation.subtract(0, heightFromSurface, 0).getBlock();
					if (BlockUtils.isPathable(b)) {
						int attempts = 0;
						boolean ok = false;
						while (attempts++ < 10) {
							b = b.getRelative(BlockFace.DOWN);
							if (BlockUtils.isPathable(b)) {
								currentLocation.add(0, -1, 0);
							} else {
								ok = true;
								break;
							}
						}
						if (!ok) {
							stop();
							return;
						}
					} else {
						int attempts = 0;
						boolean ok = false;
						while (attempts++ < 10) {
							b = b.getRelative(BlockFace.UP);
							currentLocation.add(0, 1, 0);
							if (BlockUtils.isPathable(b)) {
								ok = true;
								break;
							}
						}
						if (!ok) {
							stop();
							return;
						}
					}
					currentLocation.setY((int)currentLocation.getY() + heightFromSurface);
					currentX = currentLocation.getBlockX();
					currentZ = currentLocation.getBlockZ();
				}
			} else if (projectileGravity != 0) {
				currentVelocity.setY(currentVelocity.getY() - (projectileGravity / ticksPerSecond));
			}
			
			// show particle
			MagicSpells.getVolatileCodeHandler().playParticleEffect(currentLocation, particleName, particleXSpread, particleYSpread, particleZSpread, particleSpeed, particleCount, renderDistance, 0F);
			
			// play effects
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) {
				playSpellEffects(EffectPosition.SPECIAL, currentLocation);
			}
			
			counter++;
			
			// cast spell mid air
			if (hitAirDuring && counter % spellInterval == 0 && spell.isTargetedLocationSpell()) {
				spell.castAtLocation(caster, currentLocation.clone(), power);
			}
			
			if (stopOnHitGround && !BlockUtils.isPathable(currentLocation.getBlock())) {
				if (hitGround && spell != null && spell.isTargetedLocationSpell()) {
					Util.setLocationFacingFromVector(previousLocation, currentVelocity);
					spell.castAtLocation(caster, previousLocation, power);
					playSpellEffects(EffectPosition.TARGET, currentLocation);
				}
				stop();
			} else if (currentLocation.distanceSquared(startLocation) >= maxDistanceSquared) {
				if (hitAirAtEnd && spell != null && spell.isTargetedLocationSpell()) {
					spell.castAtLocation(caster, currentLocation.clone(), power);
					playSpellEffects(EffectPosition.TARGET, currentLocation);
				}
				stop();
			} else if (inRange != null) {
				BoundingBox hitBox = new BoundingBox(currentLocation, hitRadius, verticalHitRadius);
				for (int i = 0; i < inRange.size(); i++) {
					LivingEntity e = inRange.get(i);
					if (!e.isDead() && hitBox.contains(e.getLocation().add(0, 0.6, 0))) {
						if (spell != null) {
							if (spell.isTargetedEntitySpell()) {
								ValidTargetChecker checker = spell.getSpell().getValidTargetChecker();
								if (checker != null && !checker.isValidTarget(e)) {
									inRange.remove(i);
									break;
								}
								LivingEntity target = e;
								float thisPower = power;
								SpellTargetEvent event = new SpellTargetEvent(thisSpell, caster, target, thisPower);
								Bukkit.getPluginManager().callEvent(event);
								if (event.isCancelled()) {
									inRange.remove(i);
									break;
								} else {
									target = event.getTarget();
									thisPower = event.getPower();
								}
								spell.castAtEntity(caster, target, thisPower);
								playSpellEffects(EffectPosition.TARGET, e);
							} else if (spell.isTargetedLocationSpell()) {
								spell.castAtLocation(caster, currentLocation.clone(), power);
								playSpellEffects(EffectPosition.TARGET, currentLocation);
							}
						}
						if (stopOnHitEntity) {
							stop();
						} else {
							inRange.remove(i);
							immune.put(e, System.currentTimeMillis());
						}
						break;
					}
				}
				Iterator<Map.Entry<LivingEntity, Long>> iter = immune.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<LivingEntity, Long> entry = iter.next();
					if (entry.getValue().longValue() < System.currentTimeMillis() - 2000) {
						iter.remove();
						inRange.add(entry.getKey());
					}
				}
			}
		}
		
		public void stop() {
			playSpellEffects(EffectPosition.DELAYED, currentLocation);
			MagicSpells.cancelTask(taskId);
			caster = null;
			startLocation = null;
			previousLocation = null;
			currentLocation = null;
			currentVelocity = null;
			if (inRange != null) {
				inRange.clear();
				inRange = null;
			}
		}
		
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Location loc = target.clone();
		loc.setDirection(caster.getLocation().getDirection());
		new ProjectileTracker(caster, target, power);
		playSpellEffects(EffectPosition.CASTER, caster);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		new ProjectileTracker(null, target, power);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}
	
}
