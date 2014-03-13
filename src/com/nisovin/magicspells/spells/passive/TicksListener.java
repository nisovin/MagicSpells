package com.nisovin.magicspells.spells.passive;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class TicksListener extends PassiveListener {

	
	Map<Integer, Ticker> tickers = new HashMap<Integer, Ticker>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		try {
			int interval = Integer.parseInt(var);
			Ticker ticker = tickers.get(interval);
			if (ticker == null) {
				ticker = new Ticker(interval);
				tickers.put(interval, ticker);
			}
			ticker.add(spell);
		} catch (NumberFormatException e) {
		}
	}
	
	@Override
	public void initialize() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.isValid()) {
				for (Ticker ticker : tickers.values()) {
					ticker.add(player);
				}
			}
		}
	}
	
	@Override
	public void turnOff() {
		for (Ticker ticker : tickers.values()) {
			ticker.turnOff();
		}
		tickers.clear();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		for (Ticker ticker : tickers.values()) {
			ticker.add(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		for (Ticker ticker : tickers.values()) {
			ticker.remove(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		for (Ticker ticker : tickers.values()) {
			ticker.remove(event.getEntity());
		}
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		for (Ticker ticker : tickers.values()) {
			ticker.add(event.getPlayer());
		}
	}

	class Ticker implements Runnable {

		int taskId;
		Map<PassiveSpell, Collection<Player>> spells = new HashMap<PassiveSpell, Collection<Player>>();
		String profilingKey;
		
		public Ticker(int interval) {
			taskId = MagicSpells.scheduleRepeatingTask(this, interval, interval);
			profilingKey = MagicSpells.profilingEnabled() ? "PassiveTick:" + interval : null;
		}
		
		public void add(PassiveSpell spell) {
			spells.put(spell, new HashSet<Player>());
		}
		
		public void add(Player player) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : spells.keySet()) {
				if (spellbook.hasSpell(spell)) {
					spells.get(spell).add(player);
				}
			}
		}
		
		public void remove(Player player) {
			for (Collection<Player> players : spells.values()) {
				players.remove(player);
			}
		}
		
		@Override
		public void run() {
			long start = System.nanoTime();
			for (Map.Entry<PassiveSpell, Collection<Player>> entry : spells.entrySet()) {
				if (entry.getValue().size() > 0) {
					Iterator<Player> iter = entry.getValue().iterator();
					while (iter.hasNext()) {
						Player p = iter.next();
						if (p.isValid()) {
							entry.getKey().activate(p);
						} else {
							iter.remove();
						}
					}
				}
			}
			if (profilingKey != null) MagicSpells.addProfile(profilingKey, System.nanoTime() - start);
		}
		
		public void turnOff() {
			MagicSpells.cancelTask(taskId);
		}
		
	}
	
}
