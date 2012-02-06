package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ConjureSpell extends InstantSpell {

	private boolean addToInventory;
	private int[] itemTypes;
	private int[] itemDatas;
	private int[] itemMinQuantities;
	private int[] itemMaxQuantities;
	private int[] itemChances;
	
	public ConjureSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		addToInventory = getConfigBoolean("add-to-inventory", false);
		List<String> list = getConfigStringList("items", null);
		if (list != null && list.size() > 0) {
			itemTypes = new int[list.size()];
			itemDatas = new int[list.size()];
			itemMinQuantities = new int[list.size()];
			itemMaxQuantities = new int[list.size()];
			itemChances = new int[list.size()];
			
			for (int i = 0; i < list.size(); i++) {
				String[] data = list.get(i).split(" ");
				String[] typeData = data[0].split(":");
				String[] quantityData = data[1].split("-");
				
				if (typeData.length == 1) {
					itemTypes[i] = Integer.parseInt(typeData[0]);
					itemDatas[i] = 0;
				} else {
					itemTypes[i] = Integer.parseInt(typeData[0]);
					itemDatas[i] = Integer.parseInt(typeData[1]);
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
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get items to drop
			Random rand = new Random();
			List<ItemStack> items = new ArrayList<ItemStack>();
			for (int i = 0; i < itemTypes.length; i++) {
				if (rand.nextInt(100) < itemChances[i]) {
					int quant = itemMinQuantities[i];
					if (itemMaxQuantities[i] > itemMinQuantities[i]) {
						quant = rand.nextInt(itemMaxQuantities[i] - itemMinQuantities[i]) + itemMinQuantities[i];
					}
					ItemStack item = new ItemStack(itemTypes[i], quant, (short)itemDatas[i]);
					items.add(item);
				}
			}
			
			// drop items
			Location loc = player.getEyeLocation().add(player.getLocation().getDirection());
			for (ItemStack item : items) {
				if (addToInventory) {
					player.getInventory().addItem(item);
				} else {
					player.getWorld().dropItem(loc, item);
				}
			}
		}		
		return PostCastAction.HANDLE_NORMALLY;
		
	}
	

}
