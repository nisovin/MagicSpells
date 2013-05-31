package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.command.ScrollSpell;
import com.nisovin.magicspells.spells.command.TomeSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ConjureSpell extends InstantSpell {

	private boolean addToInventory;
	private boolean addToEnderChest;
	private boolean powerAffectsQuantity;
	private boolean powerAffectsChance;
	private boolean calculateDropsIndividually;
	private boolean autoEquip;
	List<String> itemList;
	private ItemStack[] itemTypes;
	private int[] itemMinQuantities;
	private int[] itemMaxQuantities;
	private int[] itemChances;
	
	public ConjureSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		addToInventory = getConfigBoolean("add-to-inventory", false);
		addToEnderChest = getConfigBoolean("add-to-ender-chest", false);
		powerAffectsQuantity = getConfigBoolean("power-affects-quantity", false);
		powerAffectsChance = getConfigBoolean("power-affects-chance", true);
		calculateDropsIndividually = getConfigBoolean("calculate-drops-individually", true);
		autoEquip = getConfigBoolean("auto-equip", false);
		itemList = getConfigStringList("items", null);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (itemList != null && itemList.size() > 0) {
			itemTypes = new ItemStack[itemList.size()];
			itemMinQuantities = new int[itemList.size()];
			itemMaxQuantities = new int[itemList.size()];
			itemChances = new int[itemList.size()];
			
			for (int i = 0; i < itemList.size(); i++) {
				try {
					String[] data = Util.splitParams(itemList.get(i));
					String[] quantityData = data.length == 1 ? new String[]{"1"} : data[1].split("-");
					
					if (data[0].startsWith("TOME:")) {
						String[] tomeData = data[0].split(":");
						TomeSpell tomeSpell = (TomeSpell)MagicSpells.getSpellByInternalName(tomeData[1]);
						Spell spell = MagicSpells.getSpellByInternalName(tomeData[2]);
						int uses = tomeData.length > 3 ? Integer.parseInt(tomeData[3]) : -1;
						itemTypes[i] = tomeSpell.createTome(spell, uses, null);
					} else if (data[0].startsWith("SCROLL:")) {
						String[] scrollData = data[0].split(":");
						ScrollSpell scrollSpell = (ScrollSpell)MagicSpells.getSpellByInternalName(scrollData[1]);
						Spell spell = MagicSpells.getSpellByInternalName(scrollData[2]);
						int uses = scrollData.length > 3 ? Integer.parseInt(scrollData[3]) : -1;
						itemTypes[i] = scrollSpell.createScroll(spell, uses, null);
					} else {
						itemTypes[i] = Util.getItemStackFromString(data[0]);
					}
					if (itemTypes[i] == null) {
						MagicSpells.error("Conjure spell '" + internalName + "' has specified invalid item (e1): " + itemList.get(i));
						continue;
					}
					
					if (quantityData.length == 1) {
						itemMinQuantities[i] = Integer.parseInt(quantityData[0]);
						itemMaxQuantities[i] = itemMinQuantities[i];
					} else {
						itemMinQuantities[i] = Integer.parseInt(quantityData[0]);
						itemMaxQuantities[i] = Integer.parseInt(quantityData[1]);	
					}
					
					if (data.length > 2) {
						itemChances[i] = Integer.parseInt(data[2].replace("%", ""));
					} else {
						itemChances[i] = 100;
					}
				} catch (Exception e) {
					MagicSpells.error("Conjure spell '" + internalName + "' has specified invalid item (e2): " + itemList.get(i));
					itemTypes[i] = null;
				}
			}
		}
		itemList = null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (itemTypes == null) return PostCastAction.ALREADY_HANDLED;
		if (state == SpellCastState.NORMAL) {
			
			// get items to drop
			Random rand = new Random();
			List<ItemStack> items = new ArrayList<ItemStack>();
			if (calculateDropsIndividually) {
				individual(items, rand, power);
			} else {
				together(items, rand, power);
			}
			
			// drop items
			Location loc = player.getEyeLocation().add(player.getLocation().getDirection());
			boolean updateInv = false;
			for (ItemStack item : items) {
				boolean added = false;
				PlayerInventory inv = player.getInventory();
				if (autoEquip && item.getAmount() == 1) {
					if (item.getType().name().endsWith("HELMET") && inv.getHelmet() == null) {
						inv.setHelmet(item);
						added = true;
					} else if (item.getType().name().endsWith("CHESTPLATE") && inv.getChestplate() == null) {
						inv.setChestplate(item);
						added = true;
					} else if (item.getType().name().endsWith("LEGGINGS") && inv.getLeggings() == null) {
						inv.setLeggings(item);
						added = true;
					} else if (item.getType().name().endsWith("BOOTS") && inv.getBoots() == null) {
						inv.setBoots(item);
						added = true;
					}
				}
				if (!added) {
					if (addToEnderChest) {
						added = Util.addToInventory(player.getEnderChest(), item);
					}
					if (!added && addToInventory) {
						added = Util.addToInventory(inv, item);
						if (added) updateInv = true;
					}
					if (!added) {
						player.getWorld().dropItem(loc, item).setItemStack(item);
					}
				} else {
					updateInv = true;
				}
			}
			if (updateInv) {
				player.updateInventory();
			}
			
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
		
	}
	
	private void individual(List<ItemStack> items, Random rand, float power) {
		for (int i = 0; i < itemTypes.length; i++) {
			int r = rand.nextInt(100);
			if (powerAffectsChance) r = Math.round(r / power);
			if (itemTypes[i] != null && r < itemChances[i]) {
				addItem(i, items, rand, power);
			}
		}
	}
	
	private void together(List<ItemStack> items, Random rand, float power) {
		int r = rand.nextInt(100);
		int m = 0;
		for (int i = 0; i < itemTypes.length; i++) {
			if (itemTypes[i] != null && r < itemChances[i] + m) {
				addItem(i, items, rand, power);
				return;
			} else {
				m += itemChances[i];
			}
		}
	}
	
	private void addItem(int i, List<ItemStack> items, Random rand, float power) {
		int quant = itemMinQuantities[i];
		if (itemMaxQuantities[i] > itemMinQuantities[i]) {
			quant = rand.nextInt(itemMaxQuantities[i] - itemMinQuantities[i]) + itemMinQuantities[i];
		}
		if (powerAffectsQuantity) {
			quant = Math.round(quant * power);
		}
		if (quant > 0) {
			ItemStack item = itemTypes[i].clone();
			item.setAmount(quant);
			items.add(item);
		}
	}
	

}
