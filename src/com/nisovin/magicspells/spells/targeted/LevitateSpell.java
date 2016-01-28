package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class LevitateSpell extends TargetedSpell implements TargetedEntitySpell {

	private int tickRate;
	private int duration;
	private float distanceChange;
	private float minDistance;
	private boolean cancelOnItemSwitch;
	private boolean cancelOnSpellCast;
	private boolean cancelOnTakeDamage;
	
	private HashMap<Player,Levitator> levitating;
	
	public LevitateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		tickRate = getConfigInt("tick-rate", 5);
		duration = getConfigInt("duration", 10);
		distanceChange = getConfigFloat("distance-change", 0);
		minDistance = getConfigFloat("min-distance", 1);
		cancelOnItemSwitch = getConfigBoolean("cancel-on-item-switch", true);
		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", true);
		
		levitating = new HashMap<Player,Levitator>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (cancelOnItemSwitch) {
			registerEvents(new ItemSwitchListener());
		}
		if (cancelOnSpellCast) {
			registerEvents(new SpellCastListener());
		}
		if (cancelOnTakeDamage) {
			registerEvents(new DamageListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (levitating.containsKey(player)) {
			levitating.remove(player).stop();
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			}
			
			levitate(player, target.getTarget(), target.getPower());
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void levitate(Player player, Entity target, float power) {
		double distance = player.getLocation().distance(target.getLocation());
		int duration = this.duration > 0 ? Math.round(this.duration * (20F/tickRate) * power) : 0;
		Levitator lev = new Levitator(player, target, duration, distance);
		levitating.put(player, lev);
		playSpellEffects(player, target);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		levitate(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (levitating.containsKey(event.getEntity())) {
			levitating.remove(event.getEntity()).stop();
		}
	}
	
	public class ItemSwitchListener implements Listener {
		@EventHandler
		public void onItemSwitch(PlayerItemHeldEvent event) {
			if (levitating.containsKey(event.getPlayer())) {
				levitating.remove(event.getPlayer()).stop();
			}
		}
	}
	
	public class SpellCastListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (levitating.containsKey(event.getCaster()) && !event.getSpell().getInternalName().equals(internalName)) {
				levitating.remove(event.getCaster()).stop();
			}
		}
	}
	
	public class DamageListener implements Listener {
		public void onEntityDamage(EntityDamageByEntityEvent event) {
			if (event.getEntity() instanceof Player && levitating.containsKey(event.getEntity())) {
				levitating.remove(event.getEntity()).stop();
			}
		}
	}
	
	@Override
	public void turnOff() {
		for (Levitator l : new ArrayList<Levitator>(levitating.values())) {
			l.stop();
		}
		levitating.clear();
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
				if (caster.isDead() || !caster.isOnline()) {
					stop();
				} else {
					if (distanceChange != 0 && distance > minDistance) {
						distance -= distanceChange;
						if (distance < minDistance)
							distance = minDistance;
					}
					target.setFallDistance(0);
					Vector casterLocation = caster.getEyeLocation().toVector();
					Vector targetLocation = target.getLocation().toVector();
					Vector wantedLocation = casterLocation.add(caster.getLocation().getDirection().multiply(distance));
					Vector v = wantedLocation.subtract(targetLocation).multiply(tickRate/25F + .1);
					target.setVelocity(v);
					counter++;
					if (duration > 0 && counter > duration) {
						stop();
					}
				}
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(taskId);
			stopped = true;
			levitating.remove(caster);
		}
		
	}

}
