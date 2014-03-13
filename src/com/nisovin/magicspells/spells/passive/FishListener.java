package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.Util;

public class FishListener extends PassiveListener {

	Map<EntityType, List<PassiveSpell>> types = new HashMap<EntityType, List<PassiveSpell>>();
	List<PassiveSpell> ground = new ArrayList<PassiveSpell>();
	List<PassiveSpell> fish = new ArrayList<PassiveSpell>();
	List<PassiveSpell> fail = new ArrayList<PassiveSpell>();
	List<PassiveSpell> allTypes = new ArrayList<PassiveSpell>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(spell);
		} else {
			String[] split = var.replace(" ", "").toUpperCase().split(",");
			for (String s : split) {
				if (s.equalsIgnoreCase("ground")) {
					ground.add(spell);
				} else if (s.equalsIgnoreCase("fish")) {
					fish.add(spell);
				} else if (s.equalsIgnoreCase("fail")) {
					fail.add(spell);
				} else {
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
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFish(PlayerFishEvent event) {
		PlayerFishEvent.State state = event.getState();
		Player player = event.getPlayer();
		
		if (allTypes.size() > 0) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			Entity entity = event.getCaught();
			for (PassiveSpell spell : allTypes) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player, entity instanceof LivingEntity ? (LivingEntity)entity : null);
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		}
		
		if (state == State.IN_GROUND && ground.size() > 0) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : ground) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player, event.getHook().getLocation());
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		} else if (state == State.CAUGHT_FISH && fish.size() > 0) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : fish) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player, event.getHook().getLocation());
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		} else if (state == State.FAILED_ATTEMPT && fail.size() > 0) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			for (PassiveSpell spell : fail) {
				if (spellbook.hasSpell(spell)) {
					boolean casted = spell.activate(player, event.getHook().getLocation());
					if (casted && spell.cancelDefaultAction()) {
						event.setCancelled(true);
					}
				}
			}
		} else if (state == State.CAUGHT_ENTITY && types.size() > 0) {
			Entity entity = event.getCaught();
			if (entity != null && types.containsKey(entity.getType())) {
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				for (PassiveSpell spell : fail) {
					if (spellbook.hasSpell(spell)) {
						boolean casted = spell.activate(player, entity instanceof LivingEntity ? (LivingEntity)entity : null);
						if (casted && spell.cancelDefaultAction()) {
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}
	
}
