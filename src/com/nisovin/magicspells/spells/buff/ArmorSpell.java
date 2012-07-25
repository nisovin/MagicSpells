package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ArmorSpell extends BuffSpell {

	private boolean toggle;
	private boolean permanent;
	
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	
	private String strHasArmor;
	
	private Set<Player> armored;
	
	public ArmorSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		toggle = getConfigBoolean("toggle", false);
		permanent = getConfigBoolean("permanent", false);
		
		helmet = getItem(getConfigString("helmet", ""));
		chestplate = getItem(getConfigString("chestplate", ""));
		leggings = getItem(getConfigString("leggings", ""));
		boots = getItem(getConfigString("boots", ""));
		
		strHasArmor = getConfigString("str-has-armor", "You cannot cast this spell if you are wearing armor.");
		
		armored = new HashSet<Player>();
	}
	
	@Override
	public void initialize() {
		if (!permanent) {
			registerEvents();
		}
	}
	
	private ItemStack getItem(String s) {
		if (!s.isEmpty()) {
			String[] info = s.split(" ");
			try {
				
				// get type and data
				ItemStack item = Util.getItemStackFromString(info[0]);
				item.setAmount(1);
				
				// get enchantments
				if (info.length > 1) {
					for (int i = 1; i < info.length; i++) {
						String[] enchinfo = info[i].split(":");
						Enchantment ench = null;
						if (enchinfo[0].matches("[0-9]+")) {
							ench = Enchantment.getById(Integer.parseInt(enchinfo[0]));
						} else {
							ench = Enchantment.getByName(enchinfo[0].toUpperCase());
						}
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
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (armored.contains(player)) {
			turnOff(player);
			if (toggle) {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		if (state == SpellCastState.NORMAL) {
			PlayerInventory inv = player.getInventory();
			if ((helmet != null && inv.getHelmet() != null) || (chestplate != null && inv.getChestplate() != null) || (leggings != null && inv.getLeggings() != null) || (boots != null && inv.getBoots() != null)) {
				// error
				sendMessage(player, strHasArmor);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			if (helmet != null) {
				inv.setHelmet(helmet.clone());
			}
			if (chestplate != null) {
				inv.setChestplate(chestplate.clone());
			}
			if (leggings != null) {
				inv.setLeggings(leggings.clone());
			}
			if (boots != null) {
				inv.setBoots(boots.clone());
			}
			
			if (!permanent) {
				armored.add(player);
				startSpellDuration(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player p = (Player)event.getEntity();
			if (armored.contains(p)) {
				if (event.getDamage() >= p.getHealth()) {
					// killing blow, turn off the spell
					turnOff(p);
				} else {
					// add a use per attack
					addUseAndChargeCost(p);
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getSlotType() == SlotType.ARMOR && event.getWhoClicked() instanceof Player) {
			Player p = (Player)event.getWhoClicked();
			if (armored.contains(p)) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (armored.contains(event.getPlayer())) {
			turnOff(event.getPlayer());
		}
	}
	
	@Override
	public void turnOff(Player player) {
		if (armored.contains(player)) {
			super.turnOff(player);
			armored.remove(player);
			PlayerInventory inv = player.getInventory();
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
	}

	@Override
	protected void turnOff() {
		for (Player p : new HashSet<Player>(armored)) {
			if (p.isOnline()) {
				turnOff(p);
			}
		}
	}

	@Override
	public boolean isActive(Player player) {
		return armored.contains(player);
	}

}
