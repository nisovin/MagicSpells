package com.nisovin.magicspells;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class MagicChatListener implements Listener {

	MagicSpells plugin;
	
	public MagicChatListener(MagicSpells plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onPlayerChat(final AsyncPlayerChatEvent event) {
		MagicSpells.scheduleDelayedTask(new Runnable() {
			public void run() {
				Spell spell = MagicSpells.incantations.get(event.getMessage().toLowerCase());
				if (spell != null) {
					Player player = event.getPlayer();
					Spellbook spellbook = MagicSpells.getSpellbook(player);
					if (spellbook.hasSpell(spell)) {
						spell.cast(player);
					}
				}
			}
		}, 0);
	}
	
}
