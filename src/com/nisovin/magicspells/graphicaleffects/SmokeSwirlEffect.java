package com.nisovin.magicspells.graphicaleffects;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.nisovin.magicspells.MagicSpells;

class SmokeSwirlEffect extends GraphicalEffect {

	private int[] x = {1,1,0,-1,-1,-1,0,1};
	private int[] z = {0,1,1,1,0,-1,-1,-1};
	private int[] v = {7,6,3,0,1,2,5,8};
	
	@Override
	public void playEffect(Location location, String param) {		
		new Animator(location, 1, getDuration(param));
	}
	
	@Override
	public void playEffect(Entity entity, String param) {
		new Animator(entity, 1, getDuration(param));
	}
	
	private int getDuration(String param) {
		if (param == null || param.isEmpty()) {
			return 20;
		} else {
			try {
				return Integer.parseInt(param);
			} catch (NumberFormatException e) {
				return 20;
			}
		}
	}
	
	private class Animator implements Runnable {
		
		private Entity entity;
		private Location location;
		private int interval;
		private int duration;
		private int iteration;
		private int taskId;
		
		public Animator(Location location, int interval, int duration) {
			this(interval, duration);
			this.location = location;
		}
		
		public Animator(Entity entity, int interval, int duration) {
			this(interval, duration);
			this.entity = entity;
		}
		
		public Animator(int interval, int duration) {
			this.interval = interval;
			this.duration = duration;
			this.iteration = 0;
			this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 0, interval);			
		}
		
		public void run() {
			if (iteration * interval > duration) {
				Bukkit.getScheduler().cancelTask(taskId);
			} else {
				int i = iteration % 8;
				Location loc;
				if (location != null) {
					loc = location;
				} else {
					loc = entity.getLocation();
				}
				loc.getWorld().playEffect(loc.clone().add(x[i], 0, z[i]), Effect.SMOKE, v[i]);
				iteration++;
			}
		}
	}
	
}
