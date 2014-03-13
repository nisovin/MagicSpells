package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class RespawnListener extends PassiveListener {

	List<PassiveSpell> spells = new ArrayList<PassiveSpell>();

	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		spells.add(spell);
	}
	
	@EventHandler
	public void onRespawn(PlayerRespawnEvent event) {
		if (spells.size() > 0) {
			final Player player = event.getPlayer();
			final Spellbook spellbook = MagicSpells.getSpellbook(player);
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					for (PassiveSpell spell : spells) {
						if (spellbook.hasSpell(spell)) {
							spell.activate(player);
						}
					}
				}
			}, 1);
		}
	}

}
