package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class VolleySpell extends TargetedLocationSpell {

	private int arrows;
	private int speed;
	private int spread;
	private int shootInterval;
	private int removeDelay;
	
	public VolleySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		arrows = getConfigInt("arrows", 10);
		speed = getConfigInt("speed", 20);
		spread = getConfigInt("spread", 150);
		shootInterval = getConfigInt("shoot-interval", 0);
		removeDelay = getConfigInt("remove-delay", 0);
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			
			Block target;
			try {
				target = player.getTargetBlock(null, range>0?range:100);
			} catch (IllegalStateException e) {
				target = null;
			}
			if (target == null || target.getType() == Material.AIR) {
				return noTarget(player);
			} else {
				volley(player, target.getLocation(), power);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void volley(Player player, Location target, float power) {
		Location spawn = player.getLocation();
		spawn.setY(spawn.getY()+3);
		Vector v = target.toVector().subtract(spawn.toVector()).normalize();
		
		if (shootInterval <= 0) {
			final ArrayList<Arrow> arrowList = new ArrayList<Arrow>();
			
			int arrows = Math.round(this.arrows*power);
			for (int i = 0; i < arrows; i++) {
				Arrow a = player.getWorld().spawnArrow(spawn, v, (speed/10.0F), (spread/10.0F));
				a.setVelocity(a.getVelocity());
				a.setShooter(player);
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
			new ArrowShooter(player, spawn, v);
		}
		
		playSpellEffects(player, target);
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		volley(caster, target, power);
		return true;
	}
	
	private class ArrowShooter implements Runnable {
		Player player;
		Location spawn;
		Vector dir;
		int count;
		int taskId;
		HashMap<Integer, Arrow> arrowMap;
		
		ArrowShooter(Player player, Location spawn, Vector dir) {
			this.player = player;
			this.spawn = spawn;
			this.dir = dir;
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
				Arrow a = player.getWorld().spawnArrow(spawn, dir, (speed/10.0F), (spread/10.0F));
				a.setVelocity(a.getVelocity());
				a.setShooter(player);
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