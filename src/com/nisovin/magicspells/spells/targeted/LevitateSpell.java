package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class LevitateSpell extends TargetedEntitySpell {

	private int tickRate;
	private int duration;
	
	private HashMap<Player,Levitator> levitating;
	
	public LevitateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		tickRate = getConfigInt("tick-rate", 5);
		duration = getConfigInt("duration", 10);
		
		levitating = new HashMap<Player,Levitator>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (levitating.containsKey(player)) {
			levitating.remove(player).stop();
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, range, true, true);
			if (target == null) {
				return noTarget(player);
			}
			
			levitate(player, target, power);
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void levitate(Player player, Entity target, float power) {
		double distance = player.getLocation().distance(target.getLocation());
		int duration = Math.round(this.duration * (20F/tickRate) * power);
		Levitator lev = new Levitator(player, target, duration, distance);
		levitating.put(player, lev);
		playGraphicalEffects(player, target);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		levitate(caster, target, power);
		return true;
	}
	
	private class Levitator implements Runnable {
		
		private Player caster;
		private Entity target;
		private int duration;
		private double distance;
		private int counter;
		private int taskId;
		private boolean stopped;
		
		public Levitator(Player caster, Entity target, int duration, double distance) {
			this.caster = caster;
			this.target = target;
			this.duration = duration;
			this.distance = distance;
			this.counter = 0;
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, tickRate, tickRate);
			stopped = false;
		}
		
		@Override
		public void run() {
			if (!stopped) {
				target.setFallDistance(0);
				Vector casterLocation = caster.getEyeLocation().toVector();
				Vector targetLocation = target.getLocation().toVector();
				Vector wantedLocation = casterLocation.add(caster.getLocation().getDirection().multiply(distance));
				Vector v = wantedLocation.subtract(targetLocation).multiply(tickRate/25F + .1);
				target.setVelocity(v);
				counter++;
				if (counter > duration) {
					stop();
				}
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(taskId);
			stopped = true;
		}
		
	}

}
