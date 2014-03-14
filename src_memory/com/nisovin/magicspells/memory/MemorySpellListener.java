package com.nisovin.magicspells.memory;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellLearnEvent;

public class MemorySpellListener implements Listener {

	private MagicSpellsMemory plugin;
	
	public MemorySpellListener(MagicSpellsMemory plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onSpellLearn(SpellLearnEvent event) {
		int req = plugin.getRequiredMemory(event.getSpell());
		if (req > 0) {
			int mem = plugin.getMemoryRemaining(event.getLearner());
			MagicSpells.debug("Memory check: " + req + " required, " + mem + " remaining");
			if (mem < req) {
				event.setCancelled(true);
				MagicSpells.sendMessage(event.getLearner(), plugin.strOutOfMemory, "%spell", event.getSpell().getName());
			}
		}
	}

}
