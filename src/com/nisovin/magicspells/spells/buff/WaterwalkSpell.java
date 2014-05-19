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
	
	private HashSet<String> waterwalking;
	private Ticker ticker = null;
	
	public WaterwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		speed = getConfigFloat("speed", 0.05F);
		
		waterwalking = new HashSet<String>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		waterwalking.add(player.getName());
		startTicker();
		return true;
	}

	@Override
	public boolean isActive(Player player) {
		return waterwalking.contains(player.getName());
	}

	@Override
	public void turnOffBuff(Player player) {
		if (waterwalking.remove(player.getName())) {
			player.setFlying(false);
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setAllowFlight(false);
			}
		}
		if (waterwalking.size() == 0) {
			stopTicker();
		}
	}
	
	@Override
	protected void turnOff() {
		for (String playerName : waterwalking) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null && player.isValid()) {
				player.setFlying(false);
				if (player.getGameMode() != GameMode.CREATIVE) {
					player.setAllowFlight(false);
				}
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
			for (String n : waterwalking) {
				Player p = Bukkit.getPlayerExact(n);
				if (p != null && p.isOnline() && p.isValid()) {
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
