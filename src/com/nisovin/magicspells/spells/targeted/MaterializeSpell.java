package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class MaterializeSpell extends TargetedLocationSpell {

	private int type;
	private byte data;
	private boolean applyPhysics;
	private boolean checkPlugins;
	private String strFailed;
	
	public MaterializeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String s = getConfigString("block-type", "1");
		if (s.contains(":")) {
			String[] s2 = s.split(":");
			type = Integer.parseInt(s2[0]);
			data = Byte.parseByte(s2[1]);
		} else {
			type = Integer.parseInt(s);
			data = 0;
		}
		applyPhysics = getConfigBoolean("apply-physics", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		strFailed = getConfigString("str-failed", "");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> lastTwo = null;
			try {
				lastTwo = player.getLastTwoTargetBlocks(null, range);
			} catch (IllegalStateException e) {
				lastTwo = null;
			}
			if (lastTwo != null && lastTwo.size() == 2 && lastTwo.get(1).getType() != Material.AIR && lastTwo.get(0).getType() == Material.AIR) {
				boolean done = materialize(player, lastTwo.get(0), lastTwo.get(1));
				if (!done) {
					sendMessage(player, strFailed);
					fizzle(player);
					return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
				}
			} else {
				// fail no target
				sendMessage(player, strNoTarget);
				fizzle(player);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private boolean materialize(Player player, Block block, Block against) {
		BlockState blockState = block.getState();
		
		if (checkPlugins) {
			block.setTypeIdAndData(type, data, false);
			BlockPlaceEvent event = new BlockPlaceEvent(block, blockState, against, player.getItemInHand(), player, true);
			Bukkit.getPluginManager().callEvent(event);
			blockState.update();
			if (event.isCancelled()) {
				return false;
			} else {
				block.setTypeIdAndData(type, data, applyPhysics);
			}
		} else {
			block.setTypeIdAndData(type, data, applyPhysics);
		}
		
		playGraphicalEffects(1, player);
		playGraphicalEffects(2, block.getLocation(), block.getTypeId() + "");
		
		return true;
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block b = target.getBlock();
		return materialize(caster, b, b.getRelative(BlockFace.DOWN));
	}

}
