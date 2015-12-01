package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ArmorSpell extends BuffSpell {

	private boolean permanent;
	private boolean replace;
	
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	
	private String strHasArmor;
	private String strLoreText;
	
	private Set<String> armored;
	
	public ArmorSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		permanent = getConfigBoolean("permanent", false);
		replace = getConfigBoolean("replace", false);
		
		strLoreText = ChatColor.translateAlternateColorCodes('&', getConfigString("str-lore-text", "Conjured"));
		
		helmet = getItem(getConfigString("helmet", ""));
		chestplate = getItem(getConfigString("chestplate", ""));
		leggings = getItem(getConfigString("leggings", ""));
		boots = getItem(getConfigString("boots", ""));
		
		strHasArmor = getConfigString("str-has-armor", "You cannot cast this spell if you are wearing armor.");
		
		armored = new HashSet<String>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (!permanent) {
			registerEvents(new ArmorListener());
		}
	}
	
	private ItemStack getItem(String s) {
		if (!s.isEmpty()) {
			String[] info = s.split(" ");
			try {
				
				// get type and data
				ItemStack item = Util.getItemStackFromString(info[0]);
				if (item == null) return null;
				item.setAmount(1);
				if (!permanent) {
					ItemMeta meta = item.getItemMeta();
					List<String> lore;
					if (meta.hasLore()) {
						lore = meta.getLore();
					} else {
						lore = new ArrayList<String>();
					}
					lore.add(strLoreText);
					meta.setLore(lore);
					item.setItemMeta(meta);
				}
				
				// get enchantments (left for backwards compatibility)
				if (info.length > 1) {
					for (int i = 1; i < info.length; i++) {
						String[] enchinfo = info[i].split(":");
						Enchantment ench = Util.getEnchantmentType(enchinfo[0]);
						int lvl = 1;
						if (enchinfo.length > 1) {
							lvl = Integer.parseInt(enchinfo[1].toUpperCase().replace(" ", "_"));
						}
						if (ench != null) {
							item.addUnsafeEnchantment(ench, lvl);
						}
					}
				}
				
				return item;
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		PlayerInventory inv = player.getInventory();
		if (!replace && ((helmet != null && inv.getHelmet() != null) || (chestplate != null && inv.getChestplate() != null) || (leggings != null && inv.getLeggings() != null) || (boots != null && inv.getBoots() != null))) {
			// error
			sendMessage(player, strHasArmor);
			return false;
		}
		
		setArmor(inv);
		
		if (!permanent) {
			armored.add(player.getName());
		}
		return true;
	}
	
	@Override
	public boolean recastBuff(Player player, float power, String[] args) {
		return castBuff(player, power, args);
	}
	
	private void setArmor(PlayerInventory inv) {
		if (helmet != null) {
			if (replace) inv.setHelmet(null);
			inv.setHelmet(helmet.clone());
		}
		if (chestplate != null) {
			if (replace) inv.setChestplate(null);
			inv.setChestplate(chestplate.clone());
		}
		if (leggings != null) {
			if (replace) inv.setLeggings(null);
			inv.setLeggings(leggings.clone());
		}
		if (boots != null) {
			if (replace) inv.setBoots(null);
			inv.setBoots(boots.clone());
		}
	}
	
	private void removeArmor(PlayerInventory inv) {
		if (helmet != null && inv.getHelmet() != null && inv.getHelmet().getType() == helmet.getType()) {
			inv.setHelmet(null);
		}
		if (chestplate != null && inv.getChestplate() != null && inv.getChestplate().getType() == chestplate.getType()) {
			inv.setChestplate(null);
		}
		if (leggings != null && inv.getLeggings() != null && inv.getLeggings().getType() == leggings.getType()) {
			inv.setLeggings(null);
		}
		if (boots != null && inv.getBoots() != null && inv.getBoots().getType() == boots.getType()) {
			inv.setBoots(null);
		}
	}
	
	class ArmorListener implements Listener {
	
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onEntityDamage(EntityDamageEvent event) {
			if (event.getEntity() instanceof Player) {
				Player p = (Player)event.getEntity();
				if (isActive(p) && p.getNoDamageTicks() < 10) {
					addUseAndChargeCost(p);
				}
			}
		}
		
		@EventHandler(ignoreCancelled=true)
		public void onInventoryClick(InventoryClickEvent event) {
			if (event.getSlotType() == SlotType.ARMOR && event.getWhoClicked() instanceof Player) {
				Player p = (Player)event.getWhoClicked();
				if (isActive(p)) {
					event.setCancelled(true);
				}
			}
		}
		
		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent event) {
			Iterator<ItemStack> drops = event.getDrops().iterator();
			while (drops.hasNext()) {
				ItemStack drop = drops.next();
				List<String> lore = drop.getItemMeta().getLore();
				if (lore != null && lore.size() > 0 && lore.get(lore.size()-1).equals(strLoreText)) {
					drops.remove();
				}
			}
		}
		
		@EventHandler
		public void onPlayerRespawn(PlayerRespawnEvent event) {
			if (isActive(event.getPlayer()) && !isExpired(event.getPlayer())) {
				final PlayerInventory inv = event.getPlayer().getInventory();
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						setArmor(inv);
					}
				});
			}
		}
		
		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			if (isActive(event.getPlayer())) {
				if (cancelOnLogout) {
					turnOff(event.getPlayer());
				} else {
					removeArmor(event.getPlayer().getInventory());
				}
			}
		}
		
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent event) {
			if (isActive(event.getPlayer())) {
				if (!isExpired(event.getPlayer())) {
					setArmor(event.getPlayer().getInventory());
				} else {
					turnOff(event.getPlayer());
				}
			}
		}
	}
	
	@Override
	public void turnOffBuff(Player player) {
		if (armored.remove(player.getName())) {
			if (player.isOnline()) {
				PlayerInventory inv = player.getInventory();
				removeArmor(inv);
			}
		}
	}

	@Override
	protected void turnOff() {
		for (String name : new HashSet<String>(armored)) {
			Player p = Bukkit.getPlayerExact(name);
			if (p != null && p.isOnline()) {
				turnOff(p);
			}
		}
	}

	@Override
	public boolean isActive(Player player) {
		return armored.contains(player.getName());
	}

}
