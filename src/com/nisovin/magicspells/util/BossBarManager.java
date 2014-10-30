package com.nisovin.magicspells.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.nisovin.magicspells.MagicSpells;

public class BossBarManager implements Listener {

	Map<String, String> bossBarTitles = new HashMap<String, String>();
	Map<String, Double> bossBarValues = new HashMap<String, Double>();
	
	public BossBarManager() {
		MagicSpells.registerEvents(this);
		
		MagicSpells.scheduleRepeatingTask(new Runnable() {
			public void run() {
				for (String name : bossBarTitles.keySet()) {
					Player player = Bukkit.getPlayerExact(name);
					if (player != null) {
						updateBar(player, null, 0);
					}
				}
			}
		}, 20, 20);
	}
	
	public void setPlayerBar(Player player, String title, double percent) {
		boolean alreadyShowing = bossBarTitles.containsKey(player.getName());
		bossBarTitles.put(player.getName(), title);
		bossBarValues.put(player.getName(), percent);
		
		if (alreadyShowing) {
			updateBar(player, title, percent);
		} else {
			showBar(player);
		}
	}
	
	private void showBar(Player player) {
		MagicSpells.getVolatileCodeHandler().setBossBar(player, bossBarTitles.get(player.getName()), bossBarValues.get(player.getName()));
	}
	
	private void updateBar(Player player, String title, double val) {
		MagicSpells.getVolatileCodeHandler().updateBossBar(player, title, val);
	}
	
	@EventHandler
	public void onRespawn(final PlayerRespawnEvent event) {
		if (bossBarTitles.containsKey(event.getPlayer().getName())) {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					showBar(event.getPlayer());
				}
			}, 10);
		}
	}
	
	@EventHandler
	public void onTeleport(final PlayerTeleportEvent event) {
		if (bossBarTitles.containsKey(event.getPlayer().getName())) {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					showBar(event.getPlayer());
				}
			}, 10);
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		bossBarTitles.remove(event.getPlayer().getName());
		bossBarValues.remove(event.getPlayer().getName());
	}
	
	public void turnOff() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (bossBarTitles.containsKey(player.getName())) {
				MagicSpells.getVolatileCodeHandler().removeBossBar(player);
			}
		}
		bossBarTitles.clear();
		bossBarValues.clear();
	}
	
}
