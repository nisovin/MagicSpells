package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class VolleySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private VolleySpell thisSpell;
	
	private int arrows;
	private int speed;
	private int spread;
	private int fire;
	private int shootInterval;
	private int removeDelay;
	private boolean noTarget;
	private boolean powerAffectsArrowCount;
	private boolean powerAffectsSpeed;
	
	public VolleySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		thisSpell = this;
		
		arrows = getConfigInt("arrows", 10);
		speed = getConfigInt("speed", 20);
		spread = getConfigInt("spread", 150);
		fire = getConfigInt("fire", 0);
		shootInterval = getConfigInt("shoot-interval", 0);
		removeDelay = getConfigInt("remove-delay", 0);
		noTarget = getConfigBoolean("no-target", false);
		powerAffectsArrowCount = getConfigBoolean("power-affects-arrow-count", true);
		powerAffectsSpeed = getConfigBoolean("power-affects-speed", false);
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (noTarget) {
				volley(player, player.getLocation(), null, power);
			} else {
				Block target;
				try {
					target = getTargetedBlock(player, power);
				} catch (IllegalStateException e) {
					target = null;
				}
				if (target == null || target.getType() == Material.AIR) {
					return noTarget(player);
				} else {
					volley(player, player.getLocation(), target.getLocation(), power);
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void volley(Player player, Location from, Location target, float power) {
		Location spawn = from.clone();
		spawn.setY(spawn.getY()+3);
		Vector v;
		if (noTarget || target == null) {
			v = from.getDirection();
		} else {
			v = target.toVector().subtract(spawn.toVector()).normalize();
		}
		
		if (shootInterval <= 0) {
			final ArrayList<Arrow> arrowList = new ArrayList<Arrow>();
			
			int arrows = powerAffectsArrowCount ? Math.round(this.arrows*power) : this.arrows;
			for (int i = 0; i < arrows; i++) {
				float speed = this.speed / 10F;
				if (powerAffectsSpeed) speed *= power;
				Arrow a = from.getWorld().spawnArrow(spawn, v, speed, (spread/10.0F));
				a.setVelocity(a.getVelocity());
				if (player != null) {
					a.setShooter(player);
				}
				if (fire > 0) {
					a.setFireTicks(fire);
				}
				a.setMetadata("MagicSpellsSource", new FixedMetadataValue(MagicSpells.plugin, "VolleySpell" + internalName));
				if (removeDelay > 0) arrowList.add(a);
			}
			
			if (removeDelay > 0) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						for (Arrow a : arrowList) {
							a.remove();
						}
						arrowList.clear();
					}
				}, removeDelay);
			}
			
		} else {
			new ArrowShooter(player, spawn, v, power);
		}
		
		if (player != null) {
			if (target != null) {
				playSpellEffects(player, target);
			} else {
				playSpellEffects(EffectPosition.CASTER, player);
			}
		} else {
			if (from != null) {
				playSpellEffects(EffectPosition.CASTER, from);
			}
			if (target != null) {
				playSpellEffects(EffectPosition.TARGET, target);
			}
		}
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		if (!noTarget) {
			volley(caster, caster.getLocation(), target, power);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		if (!noTarget) {
			volley(caster, from, target.getLocation(), power);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (!noTarget) {
			volley(null, from, target.getLocation(), power);
			return true;
		} else {
			return false;
		}
	}
	
	private class ArrowShooter implements Runnable {
		Player player;
		Location spawn;
		Vector dir;
		int arrows;
		float speed;
		int taskId;
		int count;
		HashMap<Integer, Arrow> arrowMap;
		
		ArrowShooter(Player player, Location spawn, Vector dir, float power) {
			this.player = player;
			this.spawn = spawn;
			this.dir = dir;
			this.arrows = powerAffectsArrowCount ? Math.round(thisSpell.arrows * power) : thisSpell.arrows;
			this.speed = thisSpell.speed / 10F;
			if (powerAffectsSpeed) this.speed *= power;
			this.count = 0;
			
			if (removeDelay > 0) {
				this.arrowMap = new HashMap<Integer, Arrow>();
			}
			
			this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 0, shootInterval);
		}
		
		@Override
		public void run() {			
			// fire an arrow
			if (count < arrows) {
				Arrow a = spawn.getWorld().spawnArrow(spawn, dir, speed, (spread/10.0F));
				a.setVelocity(a.getVelocity());
				if (player != null) {
					a.setShooter(player);
				}
				if (fire > 0) {
					a.setFireTicks(fire);
				}
				a.setMetadata("MagicSpellsSource", new FixedMetadataValue(MagicSpells.plugin, "VolleySpell" + internalName));
				if (removeDelay > 0) {
					arrowMap.put(count, a);
				}
			}
			
			// remove old arrow
			if (removeDelay > 0) {
				int old = count - removeDelay;
				if (old > 0) {
					Arrow a = arrowMap.remove(old);
					if (a != null) {
						a.remove();
					}
				}
			}
			
			// end if it's done
			if (count >= arrows + removeDelay) {
				Bukkit.getScheduler().cancelTask(taskId);
			}

			count++;
		}
	}

}