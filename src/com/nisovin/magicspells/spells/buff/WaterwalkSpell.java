package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class WaterwalkSpell extends BuffSpell {

	private float speed;
	
	private HashSet<Player> waterwalking;
	private Ticker ticker = null;
	
	public WaterwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		speed = getConfigFloat("speed", 0.05F);
		
		waterwalking = new HashSet<Player>();
	}
	
	@Override
	public void initialize() {
		
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (waterwalking.contains(player)) {
			turnOff(player);
			if (toggle) {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		if (state == SpellCastState.NORMAL) {
			waterwalking.add(player);
			startSpellDuration(player);
			startTicker();
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean isActive(Player player) {
		return waterwalking.contains(player);
	}

	@Override
	public void turnOff(Player player) {
		if (waterwalking.contains(player)) {
			super.turnOff(player);
			waterwalking.remove(player);
			player.setFlying(false);
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setAllowFlight(false);
			}
			sendMessage(player, strFade);
			if (waterwalking.size() == 0) {
				stopTicker();
			}
		}
	}
	
	@Override
	protected void turnOff() {
		for (Player player : waterwalking) {
			player.setFlying(false);
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setAllowFlight(false);
			}
		}
		waterwalking.clear();
		stopTicker();
	}
	
	private void startTicker() {
		if (ticker == null) {
			ticker = new Ticker();
		}
	}
	
	private void stopTicker() {
		if (ticker != null) {
			ticker.stop();
			ticker = null;
		}
	}
	
	private class Ticker implements Runnable {
		private int taskId = 0;
		
		private int count = 0;
		
		public Ticker() {
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 5, 5);
		}
		
		public void run() {
			count += 1;
			if (count >= 4) count = 0;
			Location loc;
			Block feet, underfeet;
			for (Player p : waterwalking) {
				if (p.isOnline() && p.isValid()) {
					loc = p.getLocation();
					feet = loc.getBlock();
					underfeet = feet.getRelative(BlockFace.DOWN);
					if (feet.getType() == Material.STATIONARY_WATER) {
						loc.setY(Math.floor(loc.getY() + 1) + .1);
						p.teleport(loc);
					} else if (p.isFlying() && underfeet.getType() == Material.AIR) {
						loc.setY(Math.floor(loc.getY() - 1) + .1);
						p.teleport(loc);
					}
					feet = p.getLocation().getBlock();
					underfeet = feet.getRelative(BlockFace.DOWN);
					if (feet.getType() == Material.AIR && underfeet.getType() == Material.STATIONARY_WATER) {
						if (!p.isFlying()) {
							p.setAllowFlight(true);
							p.setFlying(true);
							p.setFlySpeed(speed);
						}
						if (count == 0) {
							addUseAndChargeCost(p);
						}
					} else if (p.isFlying()) {
						p.setFlying(false);
						if (p.getGameMode() != GameMode.CREATIVE) {
							p.setAllowFlight(false);
						}
						p.setFlySpeed(0.1F);
					}
				}
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(taskId);
		}
	}

}
