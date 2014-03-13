package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicItemWithNameMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.PassiveSpell;

public class DropItemListener extends PassiveListener {

	Set<Material> materials = new HashSet<Material>();
	Map<MagicMaterial, List<PassiveSpell>> types = new HashMap<MagicMaterial, List<PassiveSpell>>();
	List<PassiveSpell> allTypes = new ArrayList<PassiveSpell>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(spell);
		} else {
			String[] split = var.split(",");
			for (String s : split) {
				s = s.trim();
				MagicMaterial mat = null;
				if (s.contains("|")) {
					String[] stuff = s.split("\\|");
					mat = MagicSpells.getItemNameResolver().resolveItem(stuff[0]);
					if (mat != null) {
						mat = new MagicItemWithNameMaterial(mat, stuff[1]);						
					}
				} else {
					mat = MagicSpells.getItemNameResolver().resolveItem(s);
				}
				if (mat != null) {
					List<PassiveSpell> list = types.get(mat);
					if (list == null) {
						list = new ArrayList<PassiveSpell>();
						types.put(mat, list);
					}
					list.add(spell);
					materials.add(mat.getMaterial());
				}
			}	
		}		
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent event) {
		if (allTypes.size() > 0) {
			Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
			for (PassiveSpell spell : allTypes) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(event.getPlayer());
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
		
		if (types.size() > 0) {
			List<PassiveSpell> list = getSpells(event.getItemDrop().getItemStack());
			if (list != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
				for (PassiveSpell spell : list) {
					if (spellbook.hasSpell(spell)) {
						boolean casted = spell.activate(event.getPlayer());
						if (casted && spell.cancelDefaultAction()) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
	private List<PassiveSpell> getSpells(ItemStack item) {
		if (materials.contains(item.getType())) {
			for (MagicMaterial m : types.keySet()) {
				if (m.equals(item)) {
					return types.get(m);
				}
			}
		}
		return null;
	}

}
