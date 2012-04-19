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

public class ArmorSpell extends BuffSpell {

	private boolean toggle;
	
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	
	private String strHasArmor;
	
	private Set<Player> armored;
	
	public ArmorSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		helmet = getItem(getConfigString("helmet", ""));
		chestplate = getItem(getConfigString("chestplate", ""));
		leggings = getItem(getConfigString("leggings", ""));
		boots = getItem(getConfigString("boots", ""));
		
		strHasArmor = getConfigString("str-has-armor", "You cannot cast this spell if you are wearing armor.");
		
		armored = new HashSet<Player>();
	}
	
	private ItemStack getItem(String s) {
		if (!s.isEmpty()) {
			String[] info = s.split(" ");
			int type;
			short data;
			ItemStack item;
			try {
				
				// get type and data
				if (info[0].contains(":")) {
					String[] moreinfo = info[0].split(":");
					type = Integer.parseInt(moreinfo[0]);
					data = Short.parseShort(moreinfo[1]);
				} else {
					type = Integer.parseInt(info[0]);
					data = 0;
				}
				item = new ItemStack(type, 1, data);
				
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
			
			armored.add(player);
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player p = (Player)event.getEntity();
			if (armored.contains(p)) {
				addUseAndChargeCost(p);
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
		super.turnOff(player);
		if (armored.contains(player)) {
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

}
