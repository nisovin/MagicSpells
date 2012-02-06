package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class MaterializeSpell extends TargetedSpell {

	private int type;
	private byte data;
	private boolean applyPhysics;
	private boolean checkPlugins;
	private String strNoTarget;
	
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
		strNoTarget = getConfigString("str-no-target", "No target.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> lastTwo = player.getLastTwoTargetBlocks(null, range);
			if (lastTwo.size() == 2 && lastTwo.get(1).getType() != Material.AIR && lastTwo.get(0).getType() == Material.AIR) {
				Block b = lastTwo.get(0);
				BlockState blockState = b.getState();
				b.setTypeIdAndData(type, data, applyPhysics);
				
				if (checkPlugins) {
					BlockPlaceEvent event = new BlockPlaceEvent(b, blockState, lastTwo.get(1), player.getItemInHand(), player, true);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						blockState.update();
					}
				}
			} else {
				// fail no target
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
