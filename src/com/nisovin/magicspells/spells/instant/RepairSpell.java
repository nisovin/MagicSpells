package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class RepairSpell extends InstantSpell {

	private int repairAmt;
	private String[] toRepair;
	private Set<Material> ignoreItems;
	private Set<Material> allowedItems;
	private String strNothingToRepair;
	
	public RepairSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		repairAmt = getConfigInt("repair-amount", 300);
		List<String> toRepairList = getConfigStringList("to-repair", null);
		if (toRepairList == null) {
			toRepairList = new ArrayList<String>();
		}
		if (toRepairList.size() == 0) {
			toRepairList.add("held");
		}
		Iterator<String> iter = toRepairList.iterator();
		while (iter.hasNext()) {
			String s = iter.next();
			if (!s.equals("held") && !s.equals("hotbar") && !s.equals("inventory") && !s.equals("helmet") && !s.equals("chestplate") && !s.equals("leggings") && !s.equals("boots")) {
				Bukkit.getServer().getLogger().severe("MagicSpells: repair: invalid to-repair option: " + s);
				iter.remove();
			}
		}
		toRepair = new String[toRepairList.size()];
		toRepair = toRepairList.toArray(toRepair);
		
		ignoreItems = EnumSet.noneOf(Material.class);
		List<String> list = getConfigStringList("ignore-items", null);
		if (list != null) {
			for (String s : list) {
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveItem(s);
				if (m != null && m.getMaterial() != null) {
					ignoreItems.add(m.getMaterial());
				}
			}
		}
		if (ignoreItems.size() == 0) ignoreItems = null;
		
		allowedItems = EnumSet.noneOf(Material.class);
		list = getConfigStringList("allowed-items", null);
		if (list != null) {
			for (String s : list) {
				MagicMaterial m = MagicSpells.getItemNameResolver().resolveItem(s);
				if (m != null && m.getMaterial() != null) {
					allowedItems.add(m.getMaterial());
				}
			}
		}
		if (allowedItems.size() == 0) allowedItems = null;
		
		strNothingToRepair = getConfigString("str-nothing-to-repair", "Nothing to repair.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int repaired = 0;
			for (String s : toRepair) {
				if (s.equals("held")) {
					ItemStack item = player.getItemInHand();
					if (item != null && isRepairable(item.getType()) && item.getDurability() > 0) {
						item.setDurability(newDura(item));
						player.setItemInHand(item);
						repaired++;
					}
				} else if (s.equals("hotbar") || s.equals("inventory")) {
					int start, end;
					ItemStack[] items = player.getInventory().getContents();
					if (s.equals("hotbar")) {
						start = 0; 
						end = 9;
					} else {
						start = 9; 
						end = 36;
					}
					for (int i = start; i < end; i++) {
						ItemStack item = items[i];
						if (item != null && isRepairable(item.getType()) && item.getDurability() > 0) {
							item.setDurability(newDura(item));
							items[i] = item;
							repaired++;
						}
					}
					player.getInventory().setContents(items);
				} else if (s.equals("helmet")) {
					ItemStack item = player.getInventory().getHelmet();
					if (item != null && isRepairable(item.getType()) && item.getDurability() > 0) {
						item.setDurability(newDura(item));
						player.getInventory().setHelmet(item);
						repaired++;
					}
				} else if (s.equals("chestplate")) {
					ItemStack item = player.getInventory().getChestplate();
					if (item != null && isRepairable(item.getType()) && item.getDurability() > 0) {
						item.setDurability(newDura(item));
						player.getInventory().setChestplate(item);
						repaired++;
					}
				} else if (s.equals("leggings")) {
					ItemStack item = player.getInventory().getLeggings();
					if (item != null && isRepairable(item.getType()) && item.getDurability() > 0) {
						item.setDurability(newDura(item));
						player.getInventory().setLeggings(item);
						repaired++;
					}
				} else if (s.equals("boots")) {
					ItemStack item = player.getInventory().getBoots();
					if (item != null && isRepairable(item.getType()) && item.getDurability() > 0) {
						item.setDurability(newDura(item));
						player.getInventory().setBoots(item);
						repaired++;
					}
				}
			}
			if (repaired == 0) {
				sendMessage(player, strNothingToRepair);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private short newDura(ItemStack item) {
		short dura = item.getDurability();
		dura -= repairAmt;
		if (dura < 0) dura = 0;
		return dura;
	}
	
	private boolean isRepairable(Material material) {
		if (ignoreItems != null && ignoreItems.contains(material)) return false;
		if (allowedItems != null && !allowedItems.contains(material)) return false;
		String s = material.name();
		return 
				material == Material.BOW ||
				material == Material.FLINT_AND_STEEL ||
				material == Material.SHEARS ||
				material == Material.FISHING_ROD ||
				s.endsWith("HELMET") ||
				s.endsWith("CHESTPLATE") ||
				s.endsWith("LEGGINGS") ||
				s.endsWith("BOOTS") ||
				s.endsWith("AXE") ||
				s.endsWith("HOE") ||
				s.endsWith("PICKAXE") ||
				s.endsWith("SPADE") ||
				s.endsWith("SWORD");
	}

}
