package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class FatalDamageListener extends PassiveListener {

	List<PassiveSpell> spells = new ArrayList<PassiveSpell>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		spells.add(spell);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		Player player = (Player)event.getEntity();
		if (event.getFinalDamage() >= player.getHealth()) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : spells) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player);
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
}
