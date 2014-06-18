package com.nisovin.magicspells.towny;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.CombatUtil;

public class MagicSpellsTowny extends JavaPlugin implements Listener {
	
	private Set<Spell> disallowedInTowns = new HashSet<Spell>();
	private Towny towny;
	
	@Override
	public void onEnable() {
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		Configuration config = getConfig();
		if (config.contains("disallowed-in-towns")) {
			List<String> list = config.getStringList("disallowed-in-towns");
			for (String s : list) {
				Spell spell = MagicSpells.getSpellByInternalName(s);
				if (spell == null) {
					spell = MagicSpells.getSpellByInGameName(s);
				}
				if (spell != null) {
					disallowedInTowns.add(spell);
				} else {
					getLogger().warning("Could not find spell " + s);
				}
			}
		}
		
		Plugin townyPlugin = getServer().getPluginManager().getPlugin("Towny");
		if (townyPlugin != null) {
			towny = (Towny)townyPlugin;
			getServer().getPluginManager().registerEvents(this, this);
		} else {
			getLogger().severe("Failed to find Towny");
			this.setEnabled(false);
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.getCaster() == null) return;
		boolean friendlySpell = false;
		if (event.getSpell() instanceof TargetedSpell && ((TargetedSpell)event.getSpell()).isBeneficial()) {
			friendlySpell = true;
		}
		if (!friendlySpell && CombatUtil.preventDamageCall(towny, event.getCaster(), event.getTarget())) {
			event.setCancelled(true);
		} else if (friendlySpell && event.getTarget() instanceof Player && !CombatUtil.isAlly(event.getCaster().getName(), ((Player)event.getTarget()).getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onSpellCast(SpellCastEvent event) {
		if (disallowedInTowns.contains(event.getSpell())) {
			try {
				TownyWorld world = TownyUniverse.getDataSource().getWorld(event.getCaster().getWorld().getName());
				if (world != null && world.isUsingTowny()) {
					Coord coord = Coord.parseCoord(event.getCaster());
					if (world.getTownBlock(coord) != null) {
						event.setCancelled(true);
					}
				}
			} catch (NotRegisteredException e) {
			}
		}
	}
	
}
