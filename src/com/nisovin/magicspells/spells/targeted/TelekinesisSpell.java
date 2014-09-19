package com.nisovin.magicspells.spells.targeted;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class TelekinesisSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private boolean checkPlugins;
	
	public TelekinesisSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		checkPlugins = getConfigBoolean("check-plugins", true);
		
		losTransparentBlocks = new HashSet<Byte>(losTransparentBlocks);
		losTransparentBlocks.remove((byte)Material.LEVER.getId());
		losTransparentBlocks.remove((byte)Material.STONE_PLATE.getId());
		losTransparentBlocks.remove((byte)Material.WOOD_PLATE.getId());
		losTransparentBlocks.remove((byte)Material.IRON_PLATE.getId());
		losTransparentBlocks.remove((byte)Material.GOLD_PLATE.getId());
		losTransparentBlocks.remove((byte)Material.STONE_BUTTON.getId());
		losTransparentBlocks.remove((byte)Material.WOOD_BUTTON.getId());
	}
	
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = getTargetedBlock(player, power);
			if (target == null) {
				// fail
				return noTarget(player);
			} else {
				// run target event
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, target.getLocation(), power);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					return noTarget(player);
				} else {
					target = event.getTargetLocation().getBlock();
				}
				// run effect
				boolean activated = activate(player, target);
				if (!activated) {
					return noTarget(player);
				} else {
					playSpellEffects(player, target.getLocation());
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean activate(Player caster, Block target) {
		if (target.getType() == Material.LEVER || target.getType() == Material.STONE_BUTTON || target.getType() == Material.WOOD_BUTTON) {
			if (checkPlugins(caster, target)) {
				MagicSpells.getVolatileCodeHandler().toggleLeverOrButton(target);
				return true;
			}
		} else if (target.getType() == Material.WOOD_PLATE || target.getType() == Material.STONE_PLATE || target.getType() == Material.IRON_PLATE || target.getType() == Material.GOLD_PLATE) {
			if (checkPlugins(caster, target)) {
				MagicSpells.getVolatileCodeHandler().pressPressurePlate(target);
				return true;
			}
		}		
		return false;
	}
	
	private boolean checkPlugins(Player caster, Block target) {
		if (checkPlugins) {
			PlayerInteractEvent event = new PlayerInteractEvent(caster, Action.RIGHT_CLICK_BLOCK, caster.getItemInHand(), target, BlockFace.SELF);
			Bukkit.getPluginManager().callEvent(event);
			if (event.useInteractedBlock() == Result.DENY) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		boolean activated = activate(caster, target.getBlock());
		if (activated) {
			playSpellEffects(caster, target);
		}
		return activated;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}
}