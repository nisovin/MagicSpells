package com.nisovin.magicspells.teams;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;

public class MagicSpellsTeams extends JavaPlugin implements Listener {

	private boolean useCache;
	private boolean clearCacheOnDeath;
	
	private List<Team> teams;
	private Map<String, Team> teamNames;
	private Map<String, Team> playerTeams;
	
	@Override
	public void onEnable() {
		// setup containers
		teams = new ArrayList<Team>();
		teamNames = new HashMap<String, Team>();
		playerTeams = new HashMap<String, Team>();
		
		// get config
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		reloadConfig();
		Configuration config = getConfig();
		
		// get config
		useCache = config.getBoolean("use-cache", true);
		clearCacheOnDeath = config.getBoolean("clear-cache-on-death", false);
		
		// setup teams
		MagicSpells.debug(1, "Loading teams...");
		Set<String> teamKeys = config.getConfigurationSection("teams").getKeys(false);
		for (String name : teamKeys) {
			Team team = new Team(config.getConfigurationSection("teams." + name), name);
			teams.add(team);
			teamNames.put(name, team);
			MagicSpells.debug(2, "    Team " + name + " loaded");
		}
		for (Team team : teams) {
			team.initialize(this);
		}
		
		// register events
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable() {
		teams = null;
		teamNames = null;
		playerTeams = null;
		HandlerList.unregisterAll((Plugin)this);
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.getCaster() != null && event.getTarget() instanceof Player) {
			boolean beneficial = event.getSpell().isBeneficial();
			if (!canTarget(event.getCaster(), (Player)event.getTarget())) {
				if (!beneficial) {
					event.setCancelled(true);
				}
			} else {
				if (beneficial) {
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onMagicSpellsLoad(MagicSpellsLoadedEvent event) {
		onDisable();
		onEnable();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (useCache) {
			playerTeams.remove(event.getPlayer().getName());
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (useCache && clearCacheOnDeath) {
			playerTeams.remove(event.getEntity().getName());
		}
	}
	
	public Team getTeam(Player player) {
		if (useCache) { 
			Team team = playerTeams.get(player.getName());
			if (team != null) {
				return team;
			}
		}
		for (Team team : teams) {
			if (team.inTeam(player)) {
				if (useCache) {
					playerTeams.put(player.getName(), team);
				}
				return team;
			}
		}
		return null;
	}
	
	public boolean canTarget(Player caster, Player target) {
		Team casterTeam = getTeam(caster);
		Team targetTeam = getTeam(target);
		
		// allow targeting if one of the players is not in a team
		if (casterTeam == null || targetTeam == null) {
			return true;
		}
		
		// if same team, check friendly fire
		if (casterTeam == targetTeam) {
			return casterTeam.allowFriendlyFire();
		}
		
		// otherwise check if can target
		return casterTeam.canTarget(targetTeam);
	}
	
	public Team getTeamByName(String name) {
		return teamNames.get(name);
	}
	
}
