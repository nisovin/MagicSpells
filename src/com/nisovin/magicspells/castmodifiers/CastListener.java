package com.nisovin.magicspells.castmodifiers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicspells.events.SpellCastEvent;

public class CastListener implements Listener {
	
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	public void onSpellCast(SpellCastEvent event) {
		ModifierSet m = event.getSpell().getModifiers();
		if (m != null) {
			m.apply(event);
		}
	}
	
}
