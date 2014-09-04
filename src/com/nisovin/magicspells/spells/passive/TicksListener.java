package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
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
	
	@EventHandler
	public void onLearn(SpellLearnEvent event) {
		if (event.getSpell() instanceof PassiveSpell) {
			for (Ticker ticker : tickers.values()) {
				if (ticker.monitoringSpell((PassiveSpell)event.getSpell())) {
					ticker.add(event.getLearner(), (PassiveSpell)event.getSpell());
				}
			}
		}
	}
	
	@EventHandler
	public void onForget(SpellForgetEvent event) {
		if (event.getSpell() instanceof PassiveSpell) {
			for (Ticker ticker : tickers.values()) {
				if (ticker.monitoringSpell((PassiveSpell)event.getSpell())) {
					ticker.remove(event.getForgetter(), (PassiveSpell)event.getSpell());
				}
			}
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
		
		public void add(Player player, PassiveSpell spell) {
			spells.get(spell).add(player);
		}
		
		public void remove(Player player) {
			for (Collection<Player> players : spells.values()) {
				players.remove(player);
			}
		}
		
		public void remove(Player player, PassiveSpell spell) {
			spells.get(spell).remove(player);
		}
		
		public boolean monitoringSpell(PassiveSpell spell) {
			return spells.containsKey(spell);
		}
		
		@Override
		public void run() {
			long start = System.nanoTime();
			for (Map.Entry<PassiveSpell, Collection<Player>> entry : spells.entrySet()) {
				Collection<Player> players = entry.getValue();
				if (players.size() > 0) {
					for (Player p : new ArrayList<Player>(players)) {
						if (p.isValid()) {
							entry.getKey().activate(p);
						} else {
							players.remove(p);
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
