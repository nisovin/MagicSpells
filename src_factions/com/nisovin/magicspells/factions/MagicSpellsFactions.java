package com.nisovin.magicspells.factions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.FFlag;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColls;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.UPlayer;
import com.massivecraft.mcore.ps.PS;
import com.nisovin.magicspells.events.SpellTargetEvent;

public class MagicSpellsFactions extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.getCaster() == null) return;
		if (!(event.getTarget() instanceof Player)) return;
		
		boolean beneficial = event.getSpell().isBeneficial();
		UPlayer caster = UPlayer.get(event.getCaster());
		UPlayer target = UPlayer.get(event.getTarget());
		
		Rel rel = caster.getRelationTo(target);
		if (rel.isFriend() && !beneficial) {
			event.setCancelled(true);
		} else if (!rel.isFriend() && beneficial) {
			event.setCancelled(true);
		}
		
		Faction faction = BoardColls.get().getFactionAt(PS.valueOf(event.getCaster().getLocation()));
		if (faction != null && !faction.getFlag(FFlag.PVP)) {
			event.setCancelled(true);
		}
		faction = BoardColls.get().getFactionAt(PS.valueOf(event.getTarget().getLocation()));
		if (faction != null && !faction.getFlag(FFlag.PVP)) {
			event.setCancelled(true);
		}
	}
	
}
