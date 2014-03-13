package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;

public class TeleportListener extends PassiveListener {

	Map<TeleportCause, List<PassiveSpell>> types = new HashMap<PlayerTeleportEvent.TeleportCause, List<PassiveSpell>>();
	List<PassiveSpell> allTypes = new ArrayList<PassiveSpell>(); 
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(spell);
		} else {
			String[] split = var.replace(" ", "").split(",");
			for (String s : split) {
				s = s.trim().replace("_", "");
				for (TeleportCause cause : TeleportCause.values()) {
					if (cause.name().replace("_", "").equalsIgnoreCase(s)) {
						List<PassiveSpell> list = types.get(cause);
						if (list == null) {
							list = new ArrayList<PassiveSpell>();
							types.put(cause, list);
						}
						list.add(spell);
						break;
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		
		if (allTypes.size() > 0) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : allTypes) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player);
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
		
		if (types.size() > 0 && types.containsKey(event.getCause())) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : types.get(event.getCause())) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player);
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

}
