package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class SprintListener extends PassiveListener {

	List<PassiveSpell> sprint = null;
	List<PassiveSpell> stopSprint = null;
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (trigger == PassiveTrigger.SPRINT) {
			if (sprint == null) sprint = new ArrayList<PassiveSpell>();
			sprint.add(spell);
		} else if (trigger == PassiveTrigger.STOP_SPRINT) {
			if (sprint == null) stopSprint = new ArrayList<PassiveSpell>();
			stopSprint.add(spell);
		}
	}
	
	@EventHandler
	public void onSprint(PlayerToggleSprintEvent event) {
		if (event.isSprinting()) {
			if (sprint != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
				for (PassiveSpell spell : sprint) {
					if (spellbook.hasSpell(spell, false)) {
						boolean casted = spell.activate(event.getPlayer());
						if (casted && spell.cancelDefaultAction()) {
							event.setCancelled(true);
						}
					}
				}
			}
		} else {
			if (stopSprint != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
				for (PassiveSpell spell : stopSprint) {
					if (spellbook.hasSpell(spell, false)) {
						boolean casted = spell.activate(event.getPlayer());
						if (casted && spell.cancelDefaultAction()) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}

}
