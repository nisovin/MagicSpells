package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.MaterialData;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.PassiveSpell;

public class BlockPlaceListener extends PassiveListener {

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
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
				if (m != null) {
					List<PassiveSpell> list = types.get(m);
					if (list == null) {
						list = new ArrayList<PassiveSpell>();
						types.put(m, list);
					}
					list.add(spell);
					materials.add(m.getMaterial());
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
		if (allTypes.size() > 0) {
			for (PassiveSpell spell : allTypes) {
				if (spellbook.hasSpell(spell, false)) {
					boolean casted = spell.activate(event.getPlayer(), event.getBlock().getLocation().add(0.5, 0.5, 0.5));
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
		if (types.size() > 0) {
			List<PassiveSpell> list = getSpells(event.getBlock());
			if (list != null) {
				for (PassiveSpell spell : list) {
					if (spellbook.hasSpell(spell, false)) {
						boolean casted = spell.activate(event.getPlayer(), event.getBlock().getLocation().add(0.5, 0.5, 0.5));
						if (casted && spell.cancelDefaultAction()) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
	private List<PassiveSpell> getSpells(Block block) {
		if (materials.contains(block.getType())) {
			MaterialData data = block.getState().getData();
			for (MagicMaterial m : types.keySet()) {
				if (m.equals(data)) {
					return types.get(m);
				}
			}
		}
		return null;
	}

}
