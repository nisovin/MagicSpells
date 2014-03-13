package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.Util;

public class RightClickEntityListener extends PassiveListener {

	Map<EntityType, List<PassiveSpell>> types = new HashMap<EntityType, List<PassiveSpell>>();
	List<PassiveSpell> allTypes = new ArrayList<PassiveSpell>();
	boolean ignoreCancelled = true;
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		ignoreCancelled = spell.ignoreCancelled();
		if (var == null || var.isEmpty()) {
			allTypes.add(spell);
		} else {
			String[] split = var.replace(" ", "").toUpperCase().split(",");
			for (String s : split) {
				EntityType t = Util.getEntityType(s);
				if (t != null) {
					List<PassiveSpell> list = types.get(t);
					if (list == null) {
						list = new ArrayList<PassiveSpell>();
						types.put(t, list);
					}
					list.add(spell);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onRightClickEntity(PlayerInteractEntityEvent event) {
		if (ignoreCancelled && event.isCancelled()) return;
		if (!(event.getRightClicked() instanceof LivingEntity)) return;
		if (!allTypes.isEmpty()) {
			Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
			for (PassiveSpell spell : allTypes) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(event.getPlayer(), (LivingEntity)event.getRightClicked());
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
		if (types.containsKey(event.getRightClicked().getType())) {
			Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
			List<PassiveSpell> list = types.get(event.getRightClicked().getType());
			for (PassiveSpell spell : list) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(event.getPlayer(), (LivingEntity)event.getRightClicked());
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	
	
}
