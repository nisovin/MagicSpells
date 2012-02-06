package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class FireballSpell extends TargetedSpell {
	
	private boolean requireEntityTarget;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	private float damageMultiplier;
	private boolean smallFireball;
	private boolean noExplosion;
	private boolean noExplosionEffect;
	private int noExplosionDamage;
	private int noExplosionDamageRange;
	private boolean noFire;
	private String strNoTarget;
	
	private HashMap<Fireball,Float> fireballs;
	
	public FireballSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		damageMultiplier = getConfigFloat("damage-multiplier", 0);
		smallFireball = getConfigBoolean("small-fireball", false);
		noExplosion = config.getBoolean("spells." + spellName + ".no-explosion", false);
		noExplosionEffect = getConfigBoolean("no-explosion-effect", true);
		noExplosionDamage = getConfigInt("no-explosion-damage", 5);
		noExplosionDamageRange = getConfigInt("no-explosion-damage-range", 3);
		noFire = config.getBoolean("spells." + spellName + ".no-fire", false);
		strNoTarget = config.getString("spells." + spellName + ".str-no-target", "You cannot throw a fireball there.");
		
		fireballs = new HashMap<Fireball,Float>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = player.getTargetBlock(null, range);
			if (target == null || target.getType() == Material.AIR) {
				// fail -- no target
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			} else {				
				// get a target if required
				boolean selfTarget = false;
				if (requireEntityTarget) {
					LivingEntity entity = getTargetedEntity(player, range, targetPlayers, obeyLos);
					if (entity == null) {
						sendMessage(player, strNoTarget);
						fizzle(player);
						return PostCastAction.ALREADY_HANDLED;
					} else if (entity instanceof Player && checkPlugins) {
						// run a pvp damage check
						EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, entity, DamageCause.ENTITY_ATTACK, 1);
						Bukkit.getServer().getPluginManager().callEvent(event);
						if (event.isCancelled()) {
							sendMessage(player, strNoTarget);
							fizzle(player);
							return PostCastAction.ALREADY_HANDLED;
						}
					}
					if (entity.equals(player)) {
						selfTarget = true;
					}
				}
				
				// create fireball
				Location loc;
				if (!selfTarget) {
					loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
				} else {
					loc = player.getLocation().toVector().add(player.getLocation().getDirection().setY(0).multiply(2)).toLocation(player.getWorld(), player.getLocation().getYaw()+180, 0);
				}
				Fireball fireball;
				if (smallFireball) {
					fireball = player.getWorld().spawn(loc, SmallFireball.class);
					player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
				} else {
					fireball = player.getWorld().spawn(loc, Fireball.class);
					player.getWorld().playEffect(player.getLocation(), Effect.GHAST_SHOOT, 0);
				}
				fireball.setShooter(player);
				fireballs.put(fireball,power);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onExplosionPrime(ExplosionPrimeEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (event.getEntity() instanceof Fireball) {
			Fireball fireball = (Fireball)event.getEntity();
			if (fireballs.containsKey(fireball)) {
				if (noExplosion) {
					event.setCancelled(true);
					Location loc = fireball.getLocation();
					if (noExplosionEffect) {
						loc.getWorld().createExplosion(loc, 0);
					}
					if (noExplosionDamage > 0) {
						float power = fireballs.get(fireball);
						List<Entity> inRange = fireball.getNearbyEntities(noExplosionDamageRange, noExplosionDamageRange, noExplosionDamageRange);
						for (Entity entity : inRange) {
							if (entity instanceof LivingEntity) {
								if (targetPlayers || !(entity instanceof Player)) {
									((LivingEntity)entity).damage(Math.round(noExplosionDamage * power), fireball.getShooter());
								}
							}
						}
					}
					if (!noFire) {
						final HashSet<Block> fires = new HashSet<Block>();
						for (int x = loc.getBlockX()-1; x <= loc.getBlockX()+1; x++) {
							for (int y = loc.getBlockY()-1; y <= loc.getBlockY()+1; y++) {
								for (int z = loc.getBlockZ()-1; z <= loc.getBlockZ()+1; z++) {
									if (loc.getWorld().getBlockTypeIdAt(x,y,z) == 0) {
										Block b = loc.getWorld().getBlockAt(x,y,z);
										b.setTypeIdAndData(Material.FIRE.getId(), (byte)15, false);
										fires.add(b);
									}
								}
							}						
						}
						fireball.remove();
						if (fires.size() > 0) {
							Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
								@Override
								public void run() {
									for (Block b : fires) {
										if (b.getType() == Material.FIRE) {
											b.setType(Material.AIR);
										}
									}
								}
							}, 20);
						}
					}
				} else if (noFire) {
					event.setFire(false);
				} else {
					event.setFire(true);
				}
				if (noExplosion || damageMultiplier == 0) {
					fireballs.remove(fireball);
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGH)
	public void onEntityDamage(EntityDamageEvent event) {
		if (damageMultiplier > 0 && !event.isCancelled() && event instanceof EntityDamageByEntityEvent && event.getCause() == DamageCause.ENTITY_EXPLOSION) {
			EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
			if (evt.getDamager() instanceof Fireball || evt.getDamager() instanceof SmallFireball) {
				Fireball fireball = (Fireball)evt.getDamager();
				if (fireball.getShooter() instanceof Player && fireballs.containsKey(fireball)) {
					float power = fireballs.remove(fireball);
					event.setDamage(Math.round(event.getDamage() * damageMultiplier * power));
				}
			}
		}
	}
	
}