package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class BuildSpell extends TargetedSpell {
	
	private int slot;
	private boolean consumeBlock;
	private int[] allowedTypes;
	private boolean checkPlugins;
	private String strInvalidBlock;
	private String strCantBuild;
	
	public BuildSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		slot = getConfigInt("slot", 0);
		consumeBlock = getConfigBoolean("consume-block", true);
		String[] allowed = getConfigString("allowed-types", "1,2,3,4,5,12,13,17,20,22,24,35,41,42,43,44,45,47,48,49,50,53,57,65,67,80,85,87,88,89,91,92").split(",");
		allowedTypes = new int[allowed.length];
		for (int i = 0; i < allowed.length; i++) {
			allowedTypes[i] = Integer.parseInt(allowed[i]);
		}
		checkPlugins = getConfigBoolean("check-plugins", true);
		strInvalidBlock = getConfigString("str-invalid-block", "You can't build that block.");
		strCantBuild = getConfigString("str-cant-build", "You can't build there.");
	}
	
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get mat
			ItemStack item = player.getInventory().getItem(slot);
			if (item == null || !isAllowed(item.getType())) {
				// fail
				sendMessage(player, strInvalidBlock);
				fizzle(player);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
			
			// get target
			List<Block> lastBlocks = null;
			try {
				lastBlocks = player.getLastTwoTargetBlocks(null, range);
			} catch (IllegalStateException e) {
				lastBlocks = null;
			}
			if (lastBlocks == null || lastBlocks.size() < 2 || lastBlocks.get(1).getType() == Material.AIR) {
				// fail
				sendMessage(player, strCantBuild);
				fizzle(player);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			} else {
				// check plugins
				Block b = lastBlocks.get(0);
				BlockState blockState = b.getState();
				b.setTypeIdAndData(item.getTypeId(), (byte)item.getDurability(), true);
				if (checkPlugins) {
					BlockPlaceEvent event = new BlockPlaceEvent(b, blockState, lastBlocks.get(1), player.getItemInHand(), player, true);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled() && b.getType() == item.getType()) {
						blockState.update(true);
						sendMessage(player, strCantBuild);
						fizzle(player);
						return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
					}
				}
				playGraphicalEffects(2, b.getLocation(), item.getTypeId()+"");
				if (consumeBlock) {
					int amt = item.getAmount()-1;
					if (amt > 0) {
						item.setAmount(amt);
						player.getInventory().setItem(slot, item);
					} else {
						player.getInventory().setItem(slot, null);
					}
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean isAllowed(Material mat) {
		for (int i = 0; i < allowedTypes.length; i++) {
			if (allowedTypes[i] == mat.getId()) {
				return true;
			}
		}
		return false;
	}
}