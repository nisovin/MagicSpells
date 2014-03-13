package com.nisovin.magicspells.castmodifiers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;

public class ManaListener implements Listener {
	
	@EventHandler(priority=EventPriority.LOW)
	public void onManaRegen(ManaChangeEvent event) {
		if (event.getReason() == ManaChangeReason.REGEN) {
			ModifierSet modifiers = MagicSpells.getManaHandler().getModifiers();
			if (modifiers != null) {
				modifiers.apply(event);
			}
		}
	}
	
}
