package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.util.CastItem;

public class RightClickListener implements Listener {

	MagicSpells plugin;
	
	Map<CastItem, Spell> rightClickCastItems = new HashMap<CastItem, Spell>();
	HashMap<String, Long> lastCast = new HashMap<String, Long>();
	
	public RightClickListener(MagicSpells plugin) {
		this.plugin = plugin;
		for (Spell spell : plugin.spells.values()) {
			CastItem[] items = spell.getRightClickCastItems();
			if (items.length > 0) {
				for (CastItem item : items) {
					Spell old = rightClickCastItems.put(item, spell);
					if (old != null) {
						MagicSpells.error("The spell '" + spell.getInternalName() + "' has same right-click-cast-item as '" + old.getInternalName() + "'!");
					}
				}
			}
		}
	}
	
	public boolean hasRightClickCastItems() {
		return rightClickCastItems.size() > 0;
	}
	
	@EventHandler
	public void onRightClick(final PlayerInteractEvent event) {
	    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
	    if (!event.hasItem()) return;	    

	    CastItem castItem = new CastItem(event.getItem());
	    final Spell spell = (Spell)this.rightClickCastItems.get(castItem);
	    if (spell == null) return;

	    Player player = event.getPlayer();
	    Spellbook spellbook = MagicSpells.getSpellbook(player);
	    
	    if (spellbook.hasSpell(spell) && spellbook.canCast(spell)) {
	    	
	    	if (!spell.ignoreGlobalCooldown) {
				Long lastCastTime = lastCast.get(player.getName());
				if (lastCastTime != null && lastCastTime + plugin.globalCooldown > System.currentTimeMillis()) {
					return;
				} else {
					lastCast.put(player.getName(), System.currentTimeMillis());
				}
	    	}
			
	    	MagicSpells.scheduleDelayedTask(new Runnable() {
	    		public void run() {
	    	    	spell.cast(event.getPlayer());
	    		}
	    	}, 0);
	    	event.setCancelled(true);
	    	
	    }
	}
	
}
