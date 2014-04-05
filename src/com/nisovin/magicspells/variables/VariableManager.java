package com.nisovin.magicspells.variables;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;

public class VariableManager implements Listener {

	Map<String, Variable> variables = new HashMap<String, Variable>();
	Set<String> dirtyPlayerVars = new HashSet<String>();
	boolean dirtyGlobalVars = false;
	File folder;
	
	public VariableManager(MagicSpells plugin, ConfigurationSection section) {
		if (section != null) {
			MagicSpells.debug(1, "Loading variables...");
			for (String var : section.getKeys(false)) {
				String type = section.getString(var + ".type", "global");
				double def = section.getDouble(var + ".default", 0);
				double min = section.getDouble(var + ".min", 0);
				double max = section.getDouble(var + ".max", Double.MAX_VALUE);
				boolean perm = section.getBoolean(var + ".permanent", true);
				Variable variable;
				if (type.equalsIgnoreCase("player")) {
					variable = new PlayerVariable();
				} else {
					variable = new GlobalVariable();
				}
				String scoreName = section.getString(var + ".scoreboard-title", null);
				String scorePos = section.getString(var + ".scoreboard-position", null);
				Objective objective = null;
				if (scoreName != null && scorePos != null) {
					String objName = "MSV_" + var;
					if (objName.length() > 16) objName = objName.substring(0, 16);
					objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objName);
					if (objective == null) {
						objective = Bukkit.getScoreboardManager().getMainScoreboard().registerNewObjective(objName, objName);
						objective.setDisplayName(ChatColor.translateAlternateColorCodes('&', scoreName));
						if (scorePos.equalsIgnoreCase("nameplate")) {
							objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
						} else if (scorePos.equalsIgnoreCase("playerlist")) {
							objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
						} else {
							objective.setDisplaySlot(DisplaySlot.SIDEBAR);
						}
					}
				}
				variable.init(def, min, max, perm, objective);
				variables.put(var, variable);
				MagicSpells.debug(2, "Loaded variable " + var);
			}
			MagicSpells.debug(1, variables.size() + " variables loaded!");
		}
		if (variables.size() > 0) {
			MagicSpells.registerEvents(this);
		}
		
		
		// load vars
		folder = new File(plugin.getDataFolder(), "vars");
		if (!folder.exists()) {
			folder.mkdir();
		}
		loadGlobalVars();
		for (Player player : Bukkit.getOnlinePlayers()) {
			loadPlayerVars(player.getName());
		}
		
		// start save task
		MagicSpells.scheduleRepeatingTask(new Runnable() {
			public void run() {
				if (dirtyGlobalVars) {
					saveGlobalVars();
				}
				if (dirtyPlayerVars.size() > 0) {
					saveAllPlayerVars();
				}
			}
		}, 60 * 20, 60 * 20);
	}
	
	public void modify(String variable, Player player, double amount) {
		modify(variable, player.getName(), amount);
	}
	
	public void modify(String variable, String player, double amount) {
		Variable var = variables.get(variable);
		if (var != null) {
			var.modify(player, amount);
			if (var.permanent) {
				if (var instanceof PlayerVariable) {
					dirtyPlayerVars.add(player);
				} else if (var instanceof GlobalVariable) {
					dirtyGlobalVars = true;
				}
			}
		}
	}
	
	public void set(String variable, Player player, double amount) {
		set(variable, player.getName(), amount);
	}
	
	public void set(String variable, String player, double amount) {
		Variable var = variables.get(variable);
		if (var != null) {
			var.set(player, amount);
			if (var.permanent) {
				if (var instanceof PlayerVariable) {
					dirtyPlayerVars.add(player);
				} else if (var instanceof GlobalVariable) {
					dirtyGlobalVars = true;
				}
			}
		}
	}
	
	public double getValue(String variable, Player player) {
		Variable var = variables.get(variable);
		if (var != null) {
			return var.getValue(player);
		} else {
			return 0;
		}
	}
	
	public void reset(String variable, Player player) {
		Variable var = variables.get(variable);
		if (var != null) {
			var.reset(player);
			if (var.permanent) {
				if (var instanceof PlayerVariable) {
					dirtyPlayerVars.add(player.getName());
				} else if (var instanceof GlobalVariable) {
					dirtyGlobalVars = true;
				}
			}
		}
	}
	
	private void loadGlobalVars() {
		File file = new File(folder, "GLOBAL.txt");
		if (file.exists()) {
			try {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String line = scanner.nextLine().trim();
					if (!line.isEmpty()) {
						String[] s = line.split("=");
						Variable variable = variables.get(s);
						if (variable != null && variable instanceof GlobalVariable) {
							variable.set("", Double.parseDouble(s[1]));
						}
					}
				}
				scanner.close();
			} catch (Exception e) {
				MagicSpells.error("ERROR LOADING GLOBAL VARIABLES");
				MagicSpells.handleException(e);
			}
		}	
		
		dirtyGlobalVars = false;
	}
	
	private void saveGlobalVars() {
		File file = new File(folder, "GLOBAL.txt");
		if (file.exists()) file.delete();
		
		List<String> lines = new ArrayList<String>();
		for (String variableName : variables.keySet()) {
			Variable variable = variables.get(variableName);
			if (variable instanceof GlobalVariable) {
				double val = variable.getValue("");
				if (val != variable.defaultValue) {
					lines.add(variableName + "=" + val);
				}
			}
		}
		
		if (lines.size() > 0) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file, false));
				for (String line : lines) {
					writer.write(line);
					writer.newLine();
				}
				writer.flush();
			} catch (Exception e) {
				MagicSpells.error("ERROR SAVING GLOBAL VARIABLES");
				MagicSpells.handleException(e);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (Exception e) {}
				}
			}
		}
		
		dirtyGlobalVars = false;
	}
	
	private void loadPlayerVars(String player) {
		File file = new File(folder, "PLAYER_" + player + ".txt");
		if (file.exists()) {
			try {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNext()) {
					String line = scanner.nextLine().trim();
					if (!line.isEmpty()) {
						String[] s = line.split("=");
						Variable variable = variables.get(s[0]);
						if (variable != null && variable instanceof PlayerVariable) {
							variable.set(player, Double.parseDouble(s[1]));
						}
					}
				}
				scanner.close();
			} catch (Exception e) {
				MagicSpells.error("ERROR LOADING PLAYER VARIABLES FOR " + player);
				MagicSpells.handleException(e);
			}
		}
		
		dirtyPlayerVars.remove(player);
	}
	
	private void savePlayerVars(String player) {
		File file = new File(folder, "PLAYER_" + player + ".txt");
		if (file.exists()) file.delete();
		
		List<String> lines = new ArrayList<String>();
		for (String variableName : variables.keySet()) {
			Variable variable = variables.get(variableName);
			if (variable instanceof PlayerVariable) {
				double val = variable.getValue(player);
				if (val != variable.defaultValue) {
					lines.add(variableName + "=" + val);
				}
			}
		}
		
		if (lines.size() > 0) {
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(file, false));
				for (String line : lines) {
					writer.write(line);
					writer.newLine();
				}
				writer.flush();				
			} catch (Exception e) {
				MagicSpells.error("ERROR SAVING PLAYER VARIABLES FOR " + player);
				MagicSpells.handleException(e);
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (Exception e) {}
				}
			}
		}
		
		dirtyPlayerVars.remove(player);
	}
	
	private void saveAllPlayerVars() {
		for (String player : new HashSet<String>(dirtyPlayerVars)) {
			savePlayerVars(player);
		}
	}
	
	public void disable() {
		if (dirtyGlobalVars) {
			saveGlobalVars();
		}
		if (dirtyPlayerVars.size() > 0) {
			saveAllPlayerVars();
		}
		variables.clear();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		loadPlayerVars(event.getPlayer().getName());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (dirtyPlayerVars.contains(event.getPlayer().getName())) {
			savePlayerVars(event.getPlayer().getName());
		}
	}
	
	@EventHandler
	public void onSpellCasted(SpellCastedEvent event) {
		if (event.getSpellCastState() == SpellCastState.NORMAL) {
			Map<String, Double> varMods = event.getSpell().getVariableModsCast();
			if (varMods != null && varMods.size() > 0) {
				for (String var : varMods.keySet()) {
					Variable variable = variables.get(var);
					if (variable != null) {
						Player player = event.getCaster();
						double val = varMods.get(var);
						if (val == 0) {
							variable.reset(player);
						} else {
							variable.modify(player, val);
						}
						if (variable instanceof PlayerVariable) {
							dirtyPlayerVars.add(event.getCaster().getName());
						} else if (variable instanceof GlobalVariable) {
							dirtyGlobalVars = true;
						}
						MagicSpells.debug(3, "Variable '" + var + "' for player '" + player.getName() + "' modified by " + val + " as a result of spell cast '" + event.getSpell().getName() + "'");
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent event) {
		Map<String, Double> varMods = event.getSpell().getVariableModsTarget();
		if (varMods != null && varMods.size() > 0) {
			for (String var : varMods.keySet()) {
				Variable variable = variables.get(var);
				if (variable != null) {
					Player player = event.getCaster();
					double val = varMods.get(var);
					if (val == 0) {
						variable.reset(player);
					} else {
						variable.modify(player, val);
					}
					if (variable instanceof PlayerVariable) {
						dirtyPlayerVars.add(event.getCaster().getName());
					} else if (variable instanceof GlobalVariable) {
						dirtyGlobalVars = true;
					}
					MagicSpells.debug(3, "Variable '" + var + "' for player '" + player.getName() + "' modified by " + val + " as a result of spell target from '" + event.getSpell().getName() + "'");
				}
			}
		}
	}
	
}
