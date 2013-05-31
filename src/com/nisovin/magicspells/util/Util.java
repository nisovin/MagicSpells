package com.nisovin.magicspells.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.ItemNameResolver.ItemTypeAndData;

public class Util {

	public static Map<String, ItemStack> predefinedItems = new HashMap<String, ItemStack>();
	
	public static ItemStack getItemStackFromString(String string) {
		try {
			if (predefinedItems.containsKey(string)) return predefinedItems.get(string).clone();

			ItemStack item;
			String s = string;
			String name = null;
			String[] lore = null;
			HashMap<Enchantment, Integer> enchants = null;
			int color = -1;
			if (s.contains("|")) {
				String[] temp = s.split("\\|");
				s = temp[0];
				if (temp.length == 1) {
					name = "";
				} else {
					name = ChatColor.translateAlternateColorCodes('&', temp[1].replace("__", " "));
					if (temp.length > 2) {
						lore = Arrays.copyOfRange(temp, 2, temp.length);
						for (int i = 0; i < lore.length; i++) {
							lore[i] = ChatColor.translateAlternateColorCodes('&', lore[i].replace("__", " "));
						}
					}
				}
			}
			if (s.contains(";")) {
				String[] temp = s.split(";", 2);
				s = temp[0];
				enchants = new HashMap<Enchantment, Integer>();
				if (temp[1].length() > 0) {
					String[] split = temp[1].split("\\+");
					for (int i = 0; i < split.length; i++) {
						String[] enchantData = split[i].split("-");
						Enchantment ench;
						if (enchantData[0].matches("[0-9]+")) {
							ench = Enchantment.getById(Integer.parseInt(enchantData[0]));
						} else {
							ench = Enchantment.getByName(enchantData[0].toUpperCase());
						}
						if (ench != null && enchantData[1].matches("[0-9]+")) {
							enchants.put(ench, Integer.parseInt(enchantData[1]));
						}
					}
				}
			}
			if (s.contains("#")) {
				String[] temp = s.split("#");
				s = temp[0];
				if (temp[1].matches("[0-9A-Fa-f]+")) {
					color = Integer.parseInt(temp[1], 16);
				}
			}
			ItemTypeAndData itemTypeAndData = MagicSpells.getItemNameResolver().resolve(s);
			if (itemTypeAndData != null) {
				item = new ItemStack(itemTypeAndData.id, 1, itemTypeAndData.data);
			} else {
				return null;
			}
			ItemMeta meta = item.getItemMeta();
			if (name != null) {
				meta.setDisplayName(name);
			}
			if (lore != null) {
				meta.setLore(Arrays.asList(lore));
			}
			if (color >= 0 && meta instanceof LeatherArmorMeta) {
				((LeatherArmorMeta)meta).setColor(Color.fromRGB(color));
			}
			item.setItemMeta(meta);
			if (enchants != null) {
				if (enchants.size() > 0) {
					item.addUnsafeEnchantments(enchants);
				} else {
					item = MagicSpells.getVolatileCodeHandler().addFakeEnchantment(item);
				}
			}
			return item;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void setLoreData(ItemStack item, String data) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore;
		if (meta.hasLore()) {
			lore = meta.getLore();
			if (lore.size() > 0) {
				String s = ChatColor.stripColor(lore.get(lore.size() - 1));
				if (s.startsWith("MS$")) {
					lore.remove(lore.size() - 1);
				}
			}
		} else {
			lore = new ArrayList<String>();
		}
		lore.add(ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + "MS$:" + data);
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	
	public static String getLoreData(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta.hasLore()) {
			List<String> lore = meta.getLore();
			if (lore.size() > 0) {
				String s = ChatColor.stripColor(lore.get(lore.size() - 1));
				if (s.startsWith("MS$")) {
					return s.substring(4);
				}
			}
		}
		return null;
	}
	
	public static void removeLoreData(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore;
		if (meta.hasLore()) {
			lore = meta.getLore();
			if (lore.size() > 0) {
				String s = ChatColor.stripColor(lore.get(lore.size() - 1));
				if (s.startsWith("MS$")) {
					lore.remove(lore.size() - 1);
					if (lore.size() > 0) {
						meta.setLore(lore);
					} else {
						meta.setLore(null);
					}
					item.setItemMeta(meta);
				}
			}
		}
	}
	
	public static void setFacing(Player player, Vector vector) {
		double yaw = Math.toDegrees(Math.atan2(-vector.getX(), vector.getZ()));
		double pitch = Math.toDegrees(-Math.asin(vector.getY()));
				
		Location loc = player.getLocation();
		loc.setYaw((float)yaw);
		loc.setPitch((float)pitch);
		
		player.teleport(loc);
	}
	
	public static boolean arrayContains(int[] array, int value) {
		for (int i : array) {
			if (i == value) {
				return true;
			}
		}
		return false;
	}

	public static boolean arrayContains(String[] array, String value) {
		for (String i : array) {
			if (i.equals(value)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean arrayContains(Object[] array, Object value) {
		for (Object i : array) {
			if (i != null && i.equals(value)) {
				return true;
			}
		}
		return false;
	}
	
	public static String arrayJoin(String[] array, char with) {
		if (array == null || array.length == 0) {
			return "";
		}
		int len = array.length;
		StringBuilder sb = new StringBuilder(16 + len * 8);
		sb.append(array[0]);
		for (int i = 1; i < len; i++) {
			sb.append(with);
			sb.append(array[i]);
		}
		return sb.toString();
	}
	
	public static String listJoin(List<String> list) {
		if (list == null || list.size() == 0) {
			return "";
		}
		int len = list.size();
		StringBuilder sb = new StringBuilder(len * 12);
		sb.append(list.get(0));
		for (int i = 1; i < len; i++) {
			sb.append(' ');
			sb.append(list.get(i));
		}
		return sb.toString();
	}
	
	public static String[] splitParams(String string, int max) {
		String[] words = string.trim().split(" ");
		if (words.length <= 1) {
			return words;
		}
		ArrayList<String> list = new ArrayList<String>();		
		char quote = ' ';
		String building = "";
		
		for (String word : words) {
			if (max > 0 && list.size() == max - 1) {
				if (!building.isEmpty()) building += " ";
				building += word;
			} else if (quote == ' ') {
				if (word.length() == 1 || (word.charAt(0) != '"' && word.charAt(0) != '\'')) {
					list.add(word);
				} else {
					quote = word.charAt(0);
					if (quote == word.charAt(word.length() - 1)) {
						quote = ' ';
						list.add(word.substring(1, word.length() - 1));
					} else {
						building = word.substring(1);
					}
				}
			} else {
				if (word.charAt(word.length() - 1) == quote) {
					list.add(building + " " + word.substring(0, word.length() - 1));
					building = "";
					quote = ' ';
				} else {
					building += " " + word;
				}
			}
		}
		if (!building.isEmpty()) {
			list.add(building);
		}
		return list.toArray(new String[list.size()]);
	}
	
	public static String[] splitParams(String string) {
		return splitParams(string, 0);
	}
	
	public static String[] splitParams(String[] split, int max) {
		return splitParams(arrayJoin(split, ' '), max);
	}
	
	public static String[] splitParams(String[] split) {
		return splitParams(arrayJoin(split, ' '), 0);
	}
	
	public static List<String> tabCompleteSpellName(CommandSender sender, String partial) {
		List<String> matches = new ArrayList<String>();
		if (sender instanceof Player) {
			Spellbook spellbook = MagicSpells.getSpellbook((Player)sender);
			for (Spell spell : spellbook.getSpells()) {
				if (spellbook.canTeach(spell)) {
					if (spell.getName().toLowerCase().startsWith(partial)) {
						matches.add(spell.getName());
					} else {
						String[] aliases = spell.getAliases();
						if (aliases != null && aliases.length > 0) {
							for (String alias : aliases) {
								if (alias.toLowerCase().startsWith(partial)) {
									matches.add(alias);
								}
							}
						}
					}
				}
			}
		} else if (sender.isOp()) {
			for (Spell spell : MagicSpells.spells()) {
				if (spell.getName().toLowerCase().startsWith(partial)) {
					matches.add(spell.getName());
				} else {
					String[] aliases = spell.getAliases();
					if (aliases != null && aliases.length > 0) {
						for (String alias : aliases) {
							if (alias.toLowerCase().startsWith(partial)) {
								matches.add(alias);
							}
						}
					}
				}
			}
		}
		if (matches.size() > 0) {
			return matches;
		}
		return null;
	}
	
	public static boolean removeFromInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && item.isSimilar(items[i])) {
				if (items[i].getAmount() > amt) {
					items[i].setAmount(items[i].getAmount() - amt);
					amt = 0;
					break;
				} else if (items[i].getAmount() == amt) {
					items[i] = null;
					amt = 0;
					break;
				} else {
					amt -= items[i].getAmount();
					items[i] = null;
				}
			}
		}
		if (amt == 0) {
			inventory.setContents(items);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean addToInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && item.isSimilar(items[i])) {
				if (items[i].getAmount() + amt <= items[i].getMaxStackSize()) {
					items[i].setAmount(items[i].getAmount() + amt);
					amt = 0;
					break;
				} else {
					int diff = items[i].getMaxStackSize() - items[i].getAmount();
					items[i].setAmount(items[i].getMaxStackSize());
					amt -= diff;
				}
			}
		}
		if (amt > 0) {
			for (int i = 0; i < items.length; i++) {
				if (items[i] == null) {
					if (amt > item.getMaxStackSize()) {
						items[i] = item.clone();
						items[i].setAmount(item.getMaxStackSize());
						amt -= item.getMaxStackSize();
					} else {
						items[i] = item.clone();
						items[i].setAmount(amt);
						amt = 0;
						break;
					}
				}
			}
		}
		if (amt == 0) {
			inventory.setContents(items);
			return true;
		} else {
			return false;
		}
	}
	
	public static void rotateVector(Vector v, float degrees) {
		double rad = Math.toRadians(degrees);
		double sin = Math.sin(rad);
		double cos = Math.cos(rad);
		double x = (v.getX() * cos) - (v.getZ() * sin);
		double z = (v.getX() * sin) + (v.getZ() * cos);
		v.setX(x);
		v.setZ(z);
	}
	
}
