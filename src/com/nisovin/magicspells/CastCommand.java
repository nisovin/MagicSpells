package com.nisovin.magicspells;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.util.Util;

public class CastCommand implements CommandExecutor, TabCompleter {

	MagicSpells plugin;
	boolean enableTabComplete;
	
	public CastCommand(MagicSpells plugin, boolean enableTabComplete) {
		this.plugin = plugin;
		this.enableTabComplete = enableTabComplete;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String [] args) {
		try {
			if (command.getName().equalsIgnoreCase("magicspellcast")) {
				args = Util.splitParams(args);
				if (args == null || args.length == 0) {
					if (sender instanceof Player) {
						MagicSpells.sendMessage((Player)sender, MagicSpells.strCastUsage);
					} else {
						sender.sendMessage(MagicSpells.textColor + MagicSpells.strCastUsage);
					}
				} else if (sender.isOp() && args[0].equals("forcecast") && args.length >= 3) {
					Player target = Bukkit.getPlayer(args[1]);
					if (target == null) {
						sender.sendMessage(MagicSpells.textColor + "No matching player found");
						return true;
					}
					Spell spell = MagicSpells.getSpellByInGameName(args[2]);
					if (spell == null) {
						sender.sendMessage(MagicSpells.textColor + "No such spell");
						return true;
					}
					String[] spellArgs = null;
					if (args.length > 3) {
						spellArgs = Arrays.copyOfRange(args, 3, args.length);
					}
					spell.cast(target, spellArgs);
					sender.sendMessage(MagicSpells.textColor + "Player " + target.getName() + " forced to cast " + spell.getName());
				} else if (sender.isOp() && args[0].equals("reload")) {
					if (args.length == 1) {
						plugin.unload();
						plugin.load();
						sender.sendMessage(MagicSpells.textColor + "MagicSpells config reloaded.");
					} else {
						List<Player> players = plugin.getServer().matchPlayer(args[1]);
						if (players.size() != 1) {
							sender.sendMessage(MagicSpells.textColor + "Player not found.");
						} else {
							Player player = players.get(0);
							MagicSpells.spellbooks.put(player.getName(), new Spellbook(player, plugin));
							sender.sendMessage(MagicSpells.textColor + player.getName() + "'s spellbook reloaded.");
						}
					}
				} else if (sender.isOp() && args[0].equals("resetcd")) {
					Player p = null;
					if (args.length > 1) {
						p = Bukkit.getPlayer(args[1]);
						if (p == null) {
							sender.sendMessage(MagicSpells.textColor + "No matching player found");
							return true;
						}
					}
					for (Spell spell : MagicSpells.spells.values()) {
						if (p != null) {
							spell.setCooldown(p, 0);
						} else {
							spell.getCooldowns().clear();
						}
					}
					sender.sendMessage(MagicSpells.textColor + "Cooldowns reset" + (p != null ? " for " + p.getName() : ""));
				} else if (sender.isOp() && args[0].equals("resetmana") && args.length > 1 && MagicSpells.mana != null) {
					Player p = Bukkit.getPlayer(args[1]);
					if (p != null) {
						MagicSpells.mana.createManaBar(p);
						MagicSpells.mana.addMana(p, MagicSpells.mana.getMaxMana(p), ManaChangeReason.OTHER);
						sender.sendMessage(MagicSpells.textColor + p.getName() + "'s mana reset.");
					}
				} else if (sender.isOp() && args[0].equals("profilereport")) {
					sender.sendMessage(MagicSpells.textColor + "Creating profiling report");
					MagicSpells.profilingReport();
				} else if (sender.isOp() && args[0].equals("debug")) {
					MagicSpells.debug = !MagicSpells.debug;
					sender.sendMessage("MagicSpells: debug mode " + (MagicSpells.debug?"enabled":"disabled"));
				} else if (sender instanceof Player) {
					Player player = (Player)sender;
					Spellbook spellbook = MagicSpells.getSpellbook(player);
					Spell spell = MagicSpells.getSpellByInGameName(args[0]);
					if (spell != null && spell.canCastByCommand() && spellbook.hasSpell(spell)) {
						if (spell.isValidItemForCastCommand(player.getItemInHand())) {
							String[] spellArgs = null;
							if (args.length > 1) {
								spellArgs = new String[args.length-1];
								for (int i = 1; i < args.length; i++) {
									spellArgs[i-1] = args[i];
								}
							}
							spell.cast(player, spellArgs);
						} else {
							MagicSpells.sendMessage(player, spell.getStrWrongCastItem());
						}
					} else {
						MagicSpells.sendMessage(player, MagicSpells.strUnknownSpell);
					}
				} else { // not a player
					Spell spell = MagicSpells.spellNames.get(args[0].toLowerCase());
					if (spell == null) {
						sender.sendMessage("Unknown spell.");
					} else {
						String[] spellArgs = null;
						if (args.length > 1) {
							spellArgs = new String[args.length-1];
							for (int i = 1; i < args.length; i++) {
								spellArgs[i-1] = args[i];
							}
						}
						boolean ok = spell.castFromConsole(sender, spellArgs);
						if (!ok) {
							sender.sendMessage("Cannot cast that spell from console.");
						}
					}
				}
				return true;
			} else if (command.getName().equalsIgnoreCase("magicspellmana")) {
				if (MagicSpells.enableManaBars && sender instanceof Player) {
					Player player = (Player)sender;
					MagicSpells.mana.showMana(player, true);
				}
				return true;
			}
			return false;
		} catch (Exception ex) {
			MagicSpells.handleException(ex);
			sender.sendMessage(ChatColor.RED + "An error has occured.");
			return true;
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (enableTabComplete && sender instanceof Player) {
			Spellbook spellbook = MagicSpells.getSpellbook((Player)sender);
			String partial = Util.arrayJoin(args, ' ');
			return spellbook.tabComplete(partial);
		}
		return null;
	}

}
