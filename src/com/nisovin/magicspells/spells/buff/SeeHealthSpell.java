package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.BossHealthBar;
import com.nisovin.magicspells.util.MagicConfig;

public class SeeHealthSpell extends BuffSpell {

	private boolean toggle;
	private String mode;
	private int interval;
	private int range;
	private boolean targetPlayers;
	private boolean targetNonPlayers;
	private boolean obeyLos;
	
	private HashMap<Player, BossHealthBar> bars;
	private Updater updater;
	
	public SeeHealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		toggle = getConfigBoolean("toggle", true);
		mode = getConfigString("mode", "attack");
		interval = getConfigInt("update-interval", 5);
		range = getConfigInt("range", 15);
		targetPlayers = getConfigBoolean("target-players", true);
		targetNonPlayers = getConfigBoolean("target-non-players", true);
		obeyLos = getConfigBoolean("obey-los", true);
		
		if (!mode.equals("attack") && !mode.equals("always")) {
			mode = "attack";
		}
		
		bars = new HashMap<Player, BossHealthBar>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (mode.equals("attack")) {
			registerEvents(new AttackListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (bars.containsKey(player) && toggle) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		}
		if (state == SpellCastState.NORMAL) {
			if (!bars.containsKey(player)) {
				bars.put(player, new BossHealthBar(player));
				if (updater == null && mode.equals("always")) {
					updater = new Updater();
				}
			}
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean isActive(Player player) {
		return bars.containsKey(player);
	}
	
	@Override
	public void turnOff(Player player) {
		BossHealthBar b = bars.remove(player);
		if (b != null) {
			super.turnOff(player);
			b.disable();
			sendMessage(player, strFade);
			
			if (updater != null && bars.size() == 0) {
				updater.stop();
				updater = null;
			}
		}
	}

	@Override
	protected void turnOff() {
		for (BossHealthBar bar : bars.values()) {
			bar.disable();
		}
		bars.clear();
		if (updater != null) {
			updater.stop();
			updater = null;
		}
	}
	
	class AttackListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onAttack(EntityDamageByEntityEvent event) {
			if (event.getEntity() instanceof LivingEntity) {
				Entity damager = event.getDamager();
				if (damager instanceof Projectile && ((Projectile)damager).getShooter() != null) {
					damager = ((Projectile)damager).getShooter();
				}
				BossHealthBar bar = bars.get(damager);
				if (bar != null) {
					LivingEntity e = (LivingEntity)event.getEntity();
					bar.update(e.getHealth() - event.getDamage(), e.getMaxHealth());
					addUseAndChargeCost((Player)damager);
				}
			}
		}
	}
	
	class Updater implements Runnable {
		
		private int taskId;
		
		public Updater() {
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 0, interval);
		}
		
		@Override
		public void run() {
			for (Player player : bars.keySet()) {
				LivingEntity target = getTargetedEntity(player, range, targetPlayers, targetNonPlayers, obeyLos, false);
				BossHealthBar bar = bars.get(player);
				if (target != null) {
					bar.update(target);
				} else {
					bar.disable();
				}
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(taskId);
		}
		
	}


}
