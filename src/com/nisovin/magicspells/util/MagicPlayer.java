package com.nisovin.magicspells.util;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;

public class MagicPlayer {

	private static HashMap<String, MagicPlayer> map = new HashMap<String, MagicPlayer>();
	
	public static MagicPlayer get(Player player) {
		String name = player.getName().toLowerCase();
		MagicPlayer mp = map.get(name);
		if (mp == null) {
			mp = new MagicPlayer(player);
			map.put(name, mp);
		} else if (mp.player == null || mp.player.getEntityId() != player.getEntityId()) {
			mp.set(player);
		}
		return mp;
	}
	
	public static void clear() {
		for (MagicPlayer mp : map.values()) {
			mp.player = null;
			mp.spellbook = null;
		}
		map.clear();
	}
	
	private Player player;
	private String name;
	private Spellbook spellbook;
	
	private MagicPlayer(Player player) {
		this.player = player;
		this.name = player.getName().toLowerCase();
		this.spellbook = new Spellbook(player, MagicSpells.plugin);
	}
	
	private void set() {
		this.player = Bukkit.getPlayerExact(name);
	}
	
	private void set(Player player) {
		this.player = player;
	}
	
	public void logout() {
		this.player = null;
		this.spellbook = null;
	}
	
	public Player getBukkitPlayer() {
		if (player == null) set();
		return player;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return player.getDisplayName();
	}
	
	public boolean isOnline() {
		return player.isOnline();
	}
	
	public boolean isDead() {
		return player.isDead();
	}
	
	public MagicLocation getLocation() {
		return new MagicLocation(player.getLocation());
	}
	
	public int getHealth() {
		return player.getHealth();
	}
	
	public void setHealth(int health) {
		player.setHealth(health);
	}
	
	public int getFoodLevel() {
		return player.getFoodLevel();
	}
	
	public void setFoodLevel(int foodLevel) {
		player.setFoodLevel(foodLevel);
	}
	
	public int getLevel() {
		return player.getLevel();
	}
	
	public void setLevel(int level) {
		player.setLevel(level);
	}
	
	public float getExpProgress() {
		return player.getExp();
	}
	
	public boolean hasExperience(int amount) {
		return ExperienceUtils.hasExp(player, amount);
	}
	
	public void changeExperience(int amount) {
		ExperienceUtils.changeExp(player, amount);
	}
	
	public void sendMessage(String message) {
		MagicSpells.sendMessage(player, message);
	}
	
	public void sendMessage(String message, String... replacements) {
		MagicSpells.sendMessage(player, message, replacements);
	}
	
	public Spellbook getSpellbook() {
		return spellbook;
	}
	
	public boolean canCast(Spell spell) {
		return spellbook.canCast(spell);
	}
	
	public boolean hasSpell(Spell spell) {
		return spellbook.hasSpell(spell);
	}
	
	public String getWorldName() {
		return player.getWorld().getName();
	}
	
	public boolean hasPermission(String permission) {
		return player.hasPermission(permission);
	}
	
	public boolean inventoryContains(ItemStack item) {
		int count = 0;
		ItemStack[] items = player.getInventory().getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				count += items[i].getAmount();
			}
			if (count >= item.getAmount()) {
				return true;
			}
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public void removeFromInventory(ItemStack[] itemsToRemove) {
		Inventory inventory = player.getInventory();
		ItemStack[] items = inventory.getContents();
		
		for (ItemStack item : itemsToRemove) {
			if (item != null) {
				int amt = item.getAmount();
				for (int i = 0; i < items.length; i++) {
					if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
						if (items[i].getAmount() > amt) {
							items[i].setAmount(items[i].getAmount() - amt);
							break;
						} else if (items[i].getAmount() == amt) {
							items[i] = null;
							break;
						} else {
							amt -= items[i].getAmount();
							items[i] = null;
						}
					}
				}
			}
		}
		inventory.setContents(items);
		player.updateInventory();
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
}
