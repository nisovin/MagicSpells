package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicItemWithNameMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.PassiveSpell;

public class HotBarListener extends PassiveListener {

	Set<Material> materials = new HashSet<Material>();
	Map<MagicMaterial, List<PassiveSpell>> select = new LinkedHashMap<MagicMaterial, List<PassiveSpell>>();
	Map<MagicMaterial, List<PassiveSpell>> deselect = new LinkedHashMap<MagicMaterial, List<PassiveSpell>>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		MagicMaterial mat = null;
		if (var.contains("|")) {
			String[] stuff = var.split("\\|");
			mat = MagicSpells.getItemNameResolver().resolveItem(stuff[0]);
			if (mat != null) {
				mat = new MagicItemWithNameMaterial(mat, stuff[1]);						
			}
		} else {
			mat = MagicSpells.getItemNameResolver().resolveItem(var);
		}
		if (mat != null) {
			materials.add(mat.getMaterial());
			List<PassiveSpell> list = null;
			if (trigger == PassiveTrigger.HOT_BAR_SELECT) {
				list = select.get(mat);
				if (list == null) {
					list = new ArrayList<PassiveSpell>();
					select.put(mat, list);
				}
			} else if (trigger == PassiveTrigger.HOT_BAR_DESELECT) {
				list = deselect.get(mat);
				if (list == null) {
					list = new ArrayList<PassiveSpell>();
					deselect.put(mat, list);
				}
			}
			if (list != null) {
				list.add(spell);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerScroll(PlayerItemHeldEvent event) {
		if (deselect.size() > 0) {
			ItemStack item = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
			if (item != null && item.getType() != Material.AIR) {
				List<PassiveSpell> list = getSpells(item, deselect);
				if (list != null) {
					Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
					for (PassiveSpell spell : list) {
						if (spellbook.hasSpell(spell, false)) {
							spell.activate(event.getPlayer());
						}
					}
				}
			}
		}
		if (select.size() > 0) {
			ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
			if (item != null && item.getType() != Material.AIR) {
				List<PassiveSpell> list = getSpells(item, select);
				if (list != null) {
					Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
					for (PassiveSpell spell : list) {
						if (spellbook.hasSpell(spell, false)) {
							spell.activate(event.getPlayer());
						}
					}
				}
			}
		}
	}
	
	private List<PassiveSpell> getSpells(ItemStack item, Map<MagicMaterial, List<PassiveSpell>> map) {
		if (materials.contains(item.getType())) {
			for (MagicMaterial m : map.keySet()) {
				if (m.equals(item)) {
					return map.get(m);
				}
			}
		}
		return null;
	}

}
