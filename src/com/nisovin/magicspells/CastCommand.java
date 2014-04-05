package com.nisovin.magicspells;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
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
						MagicSpells.sendMessage((Player)sender, plugin.strCastUsage);
					} else {
						sender.sendMessage(plugin.textColor + plugin.strCastUsage);
					}
				} else if (sender.isOp() && args[0].equals("forcecast") && args.length >= 3) {
					Player target = Bukkit.getPlayer(args[1]);
					if (target == null) {
						sender.sendMessage(plugin.textColor + "No matching player found");
						return true;
					}
					Spell spell = MagicSpells.getSpellByInGameName(args[2]);
					if (spell == null) {
						sender.sendMessage(plugin.textColor + "No such spell");
						return true;
					}
					String[] spellArgs = null;
					if (args.length > 3) {
						spellArgs = Arrays.copyOfRange(args, 3, args.length);
					}
					spell.cast(target, spellArgs);
					sender.sendMessage(plugin.textColor + "Player " + target.getName() + " forced to cast " + spell.getName());
				} else if (sender.isOp() && args[0].equals("reload")) {
					if (args.length == 1) {
						plugin.unload();
						plugin.load();
						sender.sendMessage(plugin.textColor + "MagicSpells config reloaded.");
					} else {
						List<Player> players = plugin.getServer().matchPlayer(args[1]);
						if (players.size() != 1) {
							sender.sendMessage(plugin.textColor + "Player not found.");
						} else {
							Player player = players.get(0);
							plugin.spellbooks.put(player.getName(), new Spellbook(player, plugin));
							sender.sendMessage(plugin.textColor + player.getName() + "'s spellbook reloaded.");
						}
					}
				} else if (sender.isOp() && args[0].equals("resetcd")) {
					Player p = null;
					if (args.length > 1) {
						p = Bukkit.getPlayer(args[1]);
						if (p == null) {
							sender.sendMessage(plugin.textColor + "No matching player found");
							return true;
						}
					}
					for (Spell spell : plugin.spells.values()) {
						if (p != null) {
							spell.setCooldown(p, 0);
						} else {
							spell.getCooldowns().clear();
						}
					}
					sender.sendMessage(plugin.textColor + "Cooldowns reset" + (p != null ? " for " + p.getName() : ""));
				} else if (sender.isOp() && args[0].equals("resetmana") && args.length > 1 && plugin.mana != null) {
					Player p = Bukkit.getPlayer(args[1]);
					if (p != null) {
						plugin.mana.createManaBar(p);
						sender.sendMessage(plugin.textColor + p.getName() + "'s mana reset.");
					}
				} else if (sender.isOp() && args[0].equals("updatemanarank") && args.length > 1 && plugin.mana != null) {
					Player p = Bukkit.getPlayer(args[1]);
					if (p != null) {
						boolean updated = plugin.mana.updateManaRankIfNecessary(p);
						plugin.mana.showMana(p);
						if (updated) {
							sender.sendMessage(plugin.textColor + p.getName() + "'s mana rank updated.");
						} else {
							sender.sendMessage(plugin.textColor + p.getName() + "'s mana rank already correct.");
						}
					}
				} else if (sender.isOp() && args[0].equals("modifyvariable") && args.length == 4) {
					String var = args[1];
					String player = args[2];
					boolean set = false;
					double num = 0;
					if (args[3].startsWith("=")) {
						set = true;
						num = Double.parseDouble(args[3].substring(1));
					} else {
						num = Double.parseDouble(args[3]);
					}
					if (set) {
						MagicSpells.getVariableManager().set(var, player, num);
					} else {
						MagicSpells.getVariableManager().modify(var, player, num);
					}
				} else if (sender.isOp() && args[0].equals("magicitem") && args.length > 1 && sender instanceof Player) {
					ItemStack item = Util.getItemStackFromString(args[1]);
					if (item != null) {
						if (args.length > 2 && args[2].matches("^[0-9]+$")) {
							item.setAmount(Integer.parseInt(args[2]));
						}
						((Player)sender).getInventory().addItem(item);
					}
				} else if (sender.isOp() && args[0].equals("download") && args.length == 3) {
					File file = new File(plugin.getDataFolder(), "spells-" + args[1] + ".yml");
					if (file.exists()) {
						sender.sendMessage(plugin.textColor + "ERROR: The file spells-" + args[1] + ".yml already exists!");
					} else {
						boolean downloaded = Util.downloadFile(args[2], file);
						if (downloaded) {
							sender.sendMessage(plugin.textColor + "SUCCESS! You will need to do a /cast reload to load the new spells.");
						} else {
							sender.sendMessage(plugin.textColor + "ERROR: The file could not be downloaded.");
						}
					}
				} else if (sender.isOp() && args[0].equals("profilereport")) {
					sender.sendMessage(plugin.textColor + "Creating profiling report");
					MagicSpells.profilingReport();
				} else if (sender.isOp() && args[0].equals("debug")) {
					plugin.debug = !plugin.debug;
					sender.sendMessage("MagicSpells: debug mode " + (plugin.debug?"enabled":"disabled"));
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
						MagicSpells.sendMessage(player, plugin.strUnknownSpell);
					}
				} else { // not a player
					Spell spell = plugin.spellNames.get(args[0].toLowerCase());
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
						boolean casted = false;
						if (sender instanceof BlockCommandSender) {
							if (spell instanceof TargetedLocationSpell) {
								Location loc = ((BlockCommandSender)sender).getBlock().getLocation().add(.5, .5, .5);
								if (spellArgs != null && spellArgs.length == 3) {
									try {
										int x = Integer.parseInt(spellArgs[0]);
										int y = Integer.parseInt(spellArgs[1]);
										int z = Integer.parseInt(spellArgs[2]);
										loc.add(x, y, z);
									} catch (NumberFormatException e) {}
								}
								((TargetedLocationSpell)spell).castAtLocation(loc, 1.0F);
								casted = true;
							}
						}
						if (!casted) {
							boolean ok = spell.castFromConsole(sender, spellArgs);
							if (!ok) {
								if ((spell instanceof TargetedEntitySpell || spell instanceof TargetedLocationSpell) && spellArgs != null && spellArgs.length == 1 && spellArgs[0].matches("^[A-Za-z0-9_]+$")) {
									Player target = Bukkit.getPlayer(spellArgs[0]);
									if (target != null) {
										if (spell instanceof TargetedEntitySpell) {
											ok = ((TargetedEntitySpell)spell).castAtEntity(target, 1.0F);
										} else if (spell instanceof TargetedLocationSpell) {
											ok = ((TargetedLocationSpell)spell).castAtLocation(target.getLocation(), 1.0F);
										}
										if (ok) {
											sender.sendMessage("Spell casted!");
										} else {
											sender.sendMessage("Spell failed, probably can't be cast from console.");
										}
									} else {
										sender.sendMessage("Invalid target.");
									}
								} else if (spell instanceof TargetedLocationSpell && spellArgs != null && spellArgs.length == 1 && spellArgs[0].matches("^[^,]+,-?[0-9]+,-?[0-9]+,-?[0-9]+$")) {
									String[] locData = spellArgs[0].split(",");
									World world = Bukkit.getWorld(locData[0]);
									if (world != null) {
										Location loc = new Location(world, Integer.parseInt(locData[1]), Integer.parseInt(locData[2]), Integer.parseInt(locData[3]));
										ok = ((TargetedLocationSpell)spell).castAtLocation(loc, 1.0F);
										if (ok) {
											sender.sendMessage("Spell casted!");
										} else {
											sender.sendMessage("Spell failed, probably can't be cast from console.");
										}
									} else {
										sender.sendMessage("No such world.");
									}
								} else {
									sender.sendMessage("Cannot cast that spell from console.");
								}
							}
						}
					}
				}
				return true;
			} else if (command.getName().equalsIgnoreCase("magicspellmana")) {
				if (plugin.enableManaBars && sender instanceof Player) {
					Player player = (Player)sender;
					plugin.mana.showMana(player, true);
				}
				return true;
			} else if (command.getName().equalsIgnoreCase("magicspellxp")) {
				if (sender instanceof Player) {
					MagicXpHandler xpHandler = plugin.magicXpHandler;
					if (xpHandler != null) {
						xpHandler.showXpInfo((Player)sender);
					}
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
