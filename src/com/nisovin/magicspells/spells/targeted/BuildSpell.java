package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class BuildSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private int slot;
	private boolean consumeBlock;
	private Material[] allowedTypes;
	private boolean checkPlugins;
	private boolean playBreakEffect;
	private String strInvalidBlock;
	private String strCantBuild;
	
	public BuildSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		slot = getConfigInt("slot", 0);
		consumeBlock = getConfigBoolean("consume-block", true);
		String[] allowed = getConfigString("allowed-types", "1,2,3,4,5,12,13,17,20,22,24,35,41,42,43,44,45,47,48,49,50,53,57,65,67,80,85,87,88,89,91,92").split(",");
		allowedTypes = new Material[allowed.length];
		for (int i = 0; i < allowed.length; i++) {
			MagicMaterial mat = MagicSpells.getItemNameResolver().resolveBlock(allowed[i]);
			if (mat != null) {
				allowedTypes[i] = mat.getMaterial();
			}
		}
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("show-effect", true);
		strInvalidBlock = getConfigString("str-invalid-block", "You can't build that block.");
		strCantBuild = getConfigString("str-cant-build", "You can't build there.");
	}
	
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get mat
			ItemStack item = player.getInventory().getItem(slot);
			if (item == null || !isAllowed(item.getType())) {
				// fail
				return noTarget(player, strInvalidBlock);
			}
			
			// get target
			List<Block> lastBlocks = null;
			try {
				lastBlocks = getLastTwoTargetedBlocks(player, power);
			} catch (IllegalStateException e) {
				lastBlocks = null;
			}
			if (lastBlocks == null || lastBlocks.size() < 2 || lastBlocks.get(1).getType() == Material.AIR) {
				// fail
				return noTarget(player, strCantBuild);
			} else {
				boolean built = build(player, lastBlocks.get(0), lastBlocks.get(1), item);
				if (!built) {
					return noTarget(player, strCantBuild);
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private boolean build(Player player, Block block, Block against, ItemStack item) {
		// check plugins
		BlockState previousState = block.getState();
		item.getData();
		BlockState state = block.getState();
		state.setType(item.getType());
		state.setData(item.getData());
		state.update(true);
		if (checkPlugins) {
			BlockPlaceEvent event = new BlockPlaceEvent(block, previousState, against, player.getItemInHand(), player, true);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled() && block.getType() == item.getType()) {
				previousState.update(true);
				return false;
			}
		}
		if (playBreakEffect) {
			block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
		}
		playSpellEffects(EffectPosition.CASTER, player);
		playSpellEffects(EffectPosition.TARGET, block.getLocation());
		playSpellEffectsTrail(player.getLocation(), block.getLocation());
		if (consumeBlock) {
			int amt = item.getAmount()-1;
			if (amt > 0) {
				item.setAmount(amt);
				player.getInventory().setItem(slot, item);
			} else {
				player.getInventory().setItem(slot, null);
			}
		}
		return true;
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		// get mat
		ItemStack item = caster.getInventory().getItem(slot);
		if (item == null || !isAllowed(item.getType())) {
			return false;
		}
		
		// get blocks
		Block block = target.getBlock();
		
		// build
		return build(caster, block, block, item);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}
	
	private boolean isAllowed(Material mat) {
		if (!mat.isBlock()) return false;
		for (int i = 0; i < allowedTypes.length; i++) {
			if (allowedTypes[i] != null && allowedTypes[i] == mat) {
				return true;
			}
		}
		return false;
	}
}