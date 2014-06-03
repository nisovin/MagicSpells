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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.PassiveSpell;

public class RightClickBlockTypeListener extends PassiveListener {

	Set<Material> materials = new HashSet<Material>();
	Map<MagicMaterial, List<PassiveSpell>> types = new HashMap<MagicMaterial, List<PassiveSpell>>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
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
			} else {
				MagicSpells.error("Invalid type on rightclickblocktype trigger '" + var + "' on passive spell '" + spell.getInternalName() + "'");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		List<PassiveSpell> list = getSpells(event.getClickedBlock());
		if (list != null) {
			Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
			for (PassiveSpell spell : list) {
				if (spell.ignoreCancelled() && event.isCancelled()) continue;
				if (spellbook.hasSpell(spell, false)) {
					boolean casted = spell.activate(event.getPlayer(), event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
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
