package com.nisovin.magicspells.spells.targeted;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class TelekinesisSpell extends TargetedLocationSpell {
	
	private HashSet<Byte> transparent;
	
	public TelekinesisSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		transparent = new HashSet<Byte>(MagicSpells.getTransparentBlocks());
		transparent.remove((byte)Material.LEVER.getId());
		transparent.remove((byte)Material.STONE_PLATE.getId());
		transparent.remove((byte)Material.WOOD_PLATE.getId());
		transparent.remove((byte)Material.STONE_BUTTON.getId());
	}
	
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = null;
			try {
				target = player.getTargetBlock(transparent, range);
			} catch (IllegalStateException e) {
				target = null;
			}
			if (target == null) {
				// fail
				sendMessage(player, strNoTarget);
				fizzle(player);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			} else {
				boolean activated = activate(target);
				if (!activated) {
					sendMessage(player, strNoTarget);
					fizzle(player);
					return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
				} else {
					playGraphicalEffects(player, target.getLocation());
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean activate(Block target) {
		if (target.getType() == Material.LEVER || target.getType() == Material.STONE_BUTTON) {
			MagicSpells.getVolatileCodeHandler().toggleLeverOrButton(target);
			return true;
		} else if (target.getType() == Material.WOOD_PLATE || target.getType() == Material.STONE_PLATE) {
			MagicSpells.getVolatileCodeHandler().pressPressurePlate(target);
			return true;
		} else {
			return false;
		}		
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		boolean activated = activate(target.getBlock());
		if (activated) {
			playGraphicalEffects(caster, target);
		}
		return activated;
	}
}