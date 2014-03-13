package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ConjureBookSpell extends InstantSpell implements TargetedLocationSpell {

	boolean addToInventory;
	ItemStack book;

	public ConjureBookSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		addToInventory = getConfigBoolean("add-to-inventory", true);
		
		String title = getConfigString("title", "Book");
		String author = getConfigString("author", "Steve");
		List<String> pages = getConfigStringList("pages", null);
		List<String> lore = getConfigStringList("lore", null);
		
		book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta)book.getItemMeta();
		meta.setTitle(ChatColor.translateAlternateColorCodes('&', title));
		meta.setAuthor(ChatColor.translateAlternateColorCodes('&', author));
		if (pages != null) {
			for (int i = 0; i < pages.size(); i++) {
				pages.set(i, ChatColor.translateAlternateColorCodes('&', pages.get(i)));
			}
			meta.setPages(pages);
		}
		if (lore != null) {
			for (int i = 0; i < lore.size(); i++) {
				lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
			}
			meta.setLore(lore);
		}
		book.setItemMeta(meta);
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			boolean added = false;
			ItemStack item = book.clone();
			if (addToInventory) {
				if (player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR) {
					player.setItemInHand(item);
					added = true;
				} else {
					added = Util.addToInventory(player.getInventory(), item);
				}
			}
			if (!added) {
				player.getWorld().dropItem(player.getLocation(), item).setItemStack(item);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return castAtLocation(target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		ItemStack item = book.clone();
		target.getWorld().dropItem(target, item).setItemStack(item);
		return true;
	}
	
}
