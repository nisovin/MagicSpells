package com.nisovin.magicspells;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;

public class MagicChatListener implements Listener {

	private MagicSpells plugin;
	
	public MagicChatListener(MagicSpells plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onPlayerChat(PlayerChatEvent event) {
		final Spell spell = MagicSpells.incantations.get(event.getMessage().toLowerCase());
		if (spell != null) {
			final Player player = event.getPlayer();
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			if (spellbook.hasSpell(spell)) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						spell.cast(player);
					}
				});
			}
		}
	}
	
}
