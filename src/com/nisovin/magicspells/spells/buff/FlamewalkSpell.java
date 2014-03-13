package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class FlamewalkSpell extends BuffSpell {
	
	private int range;
	private int fireTicks;
	private int tickInterval;
	private boolean targetPlayers;
	private boolean checkPlugins;
	
	private HashMap<String, Float> flamewalkers;
	private Burner burner;
	
	public FlamewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 8);
		fireTicks = getConfigInt("fire-ticks", 80);
		tickInterval = getConfigInt("tick-interval", 100);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		
		flamewalkers = new HashMap<String, Float>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		flamewalkers.put(player.getName(), power);
		if (burner == null) {
			burner = new Burner();
		}
		return true;
	}	
	
	@Override
	public void turnOffBuff(Player player) {
		flamewalkers.remove(player.getName());
		if (flamewalkers.size() == 0 && burner != null) {
			burner.stop();
			burner = null;
		}
	}
	
	@Override
	protected void turnOff() {
		flamewalkers.clear();
		if (burner != null) {
			burner.stop();
			burner = null;
		}
	}

	private class Burner implements Runnable {
		int taskId;
		String[] strArr = new String[0];
		
		public Burner() {
			taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, tickInterval, tickInterval);
		}
		
		public void stop() {
			Bukkit.getServer().getScheduler().cancelTask(taskId);
		}
		
		public void run() {
			for (String s : flamewalkers.keySet().toArray(strArr)) {
				Player player = Bukkit.getServer().getPlayer(s);
				float power = flamewalkers.get(s);
				if (player != null) {
					if (isExpired(player)) {
						turnOff(player);
						continue;
					}
					playSpellEffects(EffectPosition.DELAYED, player);
					List<Entity> entities = player.getNearbyEntities(range, range, range);
					for (Entity entity : entities) {
						if (entity instanceof Player) {
							if (entity != player && targetPlayers) {
								if (checkPlugins) {
									EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, entity, DamageCause.ENTITY_ATTACK, (double)1);
									Bukkit.getServer().getPluginManager().callEvent(event);
									if (event.isCancelled()) {
										continue;
									}
								}
							}
						} else if (!(entity instanceof LivingEntity)) {
							continue;
						}
						entity.setFireTicks(Math.round(fireTicks*power));
						playSpellEffects(EffectPosition.TARGET, entity);
						playSpellEffectsTrail(player.getLocation(), entity.getLocation());
						addUse(player);
						chargeUseCost(player);
					}
				}
			}
		}
	}

	@Override
	public boolean isActive(Player player) {
		return flamewalkers.containsKey(player.getName());
	}



}