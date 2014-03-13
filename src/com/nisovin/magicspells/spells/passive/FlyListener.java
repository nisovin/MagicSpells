package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class FlyListener extends PassiveListener {

	List<PassiveSpell> fly = null;
	List<PassiveSpell> stopFly = null;
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (trigger == PassiveTrigger.FLY) {
			if (fly == null) fly = new ArrayList<PassiveSpell>();
			fly.add(spell);
		} else if (trigger == PassiveTrigger.STOP_FLY) {
			if (stopFly == null) stopFly = new ArrayList<PassiveSpell>();
			stopFly.add(spell);
		}
	}
	
	@EventHandler
	public void onFly(PlayerToggleFlightEvent event) {
		if (event.isFlying()) {
			if (fly != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
				for (PassiveSpell spell : fly) {
					if (spellbook.hasSpell(spell, false)) {
						boolean casted = spell.activate(event.getPlayer());
						if (casted && spell.cancelDefaultAction()) {
							event.setCancelled(true);
						}
					}
				}
			}
		} else {
			if (stopFly != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
				for (PassiveSpell spell : stopFly) {
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
