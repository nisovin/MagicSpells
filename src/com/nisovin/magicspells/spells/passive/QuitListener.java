package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class QuitListener extends PassiveListener {

	List<PassiveSpell> spells = new ArrayList<PassiveSpell>();

	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		spells.add(spell);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		for (PassiveSpell spell : spells) {
			if (spellbook.hasSpell(spell)) {
				spell.activate(player);
			}
		}
	}

}
