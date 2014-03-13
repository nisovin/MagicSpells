package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class DeathListener extends PassiveListener {

	List<PassiveSpell> spells = new ArrayList<PassiveSpell>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		spells.add(spell);
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Spellbook spellbook = MagicSpells.getSpellbook(event.getEntity());
		for (PassiveSpell spell : spells) {
			if (spellbook.hasSpell(spell)) {
				spell.activate(event.getEntity());
			}
		}
	}

}
