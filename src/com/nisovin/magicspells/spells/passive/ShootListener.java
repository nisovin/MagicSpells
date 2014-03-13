package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class ShootListener extends PassiveListener {

	List<PassiveSpell> spells = new ArrayList<PassiveSpell>();

	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		spells.add(spell);
	}
	
	@EventHandler
	public void onShoot(final EntityShootBowEvent event) {
		if (spells.size() > 0 && event.getEntity() instanceof Player) {
			Player player = (Player)event.getEntity();
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : spells) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player, event.getForce());
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
						event.getProjectile().remove();
					}
				}
			}
		}
	}
}
