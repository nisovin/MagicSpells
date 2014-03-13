package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class SneakListener extends PassiveListener {

	List<PassiveSpell> sneak = null;
	List<PassiveSpell> stopSneak = null;
		
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (trigger == PassiveTrigger.SNEAK) {
			if (sneak == null) sneak = new ArrayList<PassiveSpell>();
			sneak.add(spell);
		} else if (trigger == PassiveTrigger.STOP_SNEAK) {
			if (stopSneak == null) stopSneak = new ArrayList<PassiveSpell>();
			stopSneak.add(spell);
		}
	}
	
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		if (event.isSneaking()) {
			if (sneak != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
				for (PassiveSpell spell : sneak) {
					if (spellbook.hasSpell(spell, false)) {
						spell.activate(event.getPlayer());
					}
				}
			}
		} else {
			if (stopSneak != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
				for (PassiveSpell spell : stopSneak) {
					if (spellbook.hasSpell(spell, false)) {
						spell.activate(event.getPlayer());
					}
				}
			}
		}
	}

}
