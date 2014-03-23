package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class LifeLengthTracker implements Listener {

	Map<String, Long> lastSpawn = new HashMap<String, Long>();
	Map<String, Integer> lastLifeLength = new HashMap<String, Integer>();
	
	public LifeLengthTracker() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			lastSpawn.put(player.getName(), System.currentTimeMillis());
		}
		MagicSpells.registerEvents(this);
	}
	
	public int getCurrentLifeLength(Player player) {
		if (lastSpawn.containsKey(player.getName())) {
			long spawn = lastSpawn.get(player.getName());
			return (int)((System.currentTimeMillis() - spawn) / 1000);
		} else {
			return 0;
		}
	}
	
	public int getLastLifeLength(Player player) {
		if (lastLifeLength.containsKey(player.getName())) {
			return lastLifeLength.get(player.getName());
		} else {
			return 0;
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		lastSpawn.put(event.getPlayer().getName(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Long spawn = lastSpawn.remove(event.getPlayer().getName());
		if (spawn != null) {
			lastLifeLength.put(event.getPlayer().getName(), (int)((System.currentTimeMillis() - spawn) / 1000));
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Long spawn = lastSpawn.remove(event.getEntity().getName());
		if (spawn != null) {
			lastLifeLength.put(event.getEntity().getName(), (int)((System.currentTimeMillis() - spawn) / 1000));
		}
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		lastSpawn.put(event.getPlayer().getName(), System.currentTimeMillis());
	}
	
	
}
