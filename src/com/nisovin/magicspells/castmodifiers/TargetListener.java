package com.nisovin.magicspells.castmodifiers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class TargetListener implements Listener {
	
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	public void onSpellTarget(SpellTargetEvent event) {
		ModifierSet m = event.getSpell().getTargetModifiers();
		if (m != null) {
			m.apply(event);
		}
	}
	
	@EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
	public void onSpellTarget(SpellTargetLocationEvent event) {
		ModifierSet m = event.getSpell().getTargetModifiers();
		if (m != null) {
			m.apply(event);
		}
	}
	
}
