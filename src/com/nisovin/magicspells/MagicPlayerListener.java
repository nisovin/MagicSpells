package com.nisovin.magicspells;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class MagicPlayerListener implements Listener {
	
	private MagicSpells plugin;
	
	public MagicPlayerListener(MagicSpells plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {		
		// set up spell book
		Spellbook spellbook = new Spellbook(event.getPlayer(), plugin);
		MagicSpells.spellbooks.put(event.getPlayer().getName(), spellbook);
		
		// set up mana bar
		if (MagicSpells.mana != null) {
			MagicSpells.mana.createManaBar(event.getPlayer());
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		MagicSpells.spellbooks.remove(event.getPlayer().getName());
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		if (MagicSpells.separatePlayerSpellsPerWorld) {
			MagicSpells.debug(2, "Player '" + event.getPlayer().getName() + "' changed from world '" + event.getFrom().getName() + "' to '" + event.getPlayer().getWorld().getName() + "', reloading spells");
			MagicSpells.getSpellbook(event.getPlayer()).reload();
		}
	}
	
}
