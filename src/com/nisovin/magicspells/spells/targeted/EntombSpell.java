package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class EntombSpell extends TargetedSpell implements TargetedEntitySpell {

	private MagicMaterial tombBlockType;
	private int tombDuration;
	private boolean closeTopAndBottom;
	private boolean allowBreaking;
	
	private HashSet<Block> blocks;
	
	public EntombSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tombBlockType = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("tomb-block-type", "glass"));
		tombDuration = getConfigInt("tomb-duration", 20);
		closeTopAndBottom = getConfigBoolean("close-top-and-bottom", true);
		allowBreaking = getConfigBoolean("allow-breaking", true);
		
		blocks = new HashSet<Block>();
		
		if (tombBlockType == null) {
			MagicSpells.error("Entomb spell '" + spellName + "' has an invalid tomb-block-type!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
			if (targetInfo != null) {
				LivingEntity target = targetInfo.getTarget();
				power = targetInfo.getPower();
				int x = target.getLocation().getBlockX();
				int y = target.getLocation().getBlockY();
				int z = target.getLocation().getBlockZ();
				
				Location loc = new Location(target.getLocation().getWorld(), x+.5, y+.5, z+.5, target.getLocation().getYaw(), target.getLocation().getPitch());
				target.teleport(loc);
				
				createTomb(target, power);
				playSpellEffects(player, target);
				sendMessages(player, target);
				return PostCastAction.NO_MESSAGES;
			} else {
				return noTarget(player);
			}
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void createTomb(LivingEntity target, float power) {		
		ArrayList<Block> tombBlocks = new ArrayList<Block>();
		Block feet = target.getLocation().getBlock();
		
		Block temp = feet.getRelative(1,0,0);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		temp = feet.getRelative(1,1,0);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		temp = feet.getRelative(-1,0,0);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		temp = feet.getRelative(-1,1,0);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		temp = feet.getRelative(0,0,1);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		temp = feet.getRelative(0,1,1);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		temp = feet.getRelative(0,0,-1);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		temp = feet.getRelative(0,1,-1);
		if (temp.getType() == Material.AIR) {
			tombBlockType.setBlock(temp);
			tombBlocks.add(temp);
		}
		if (closeTopAndBottom) {
			temp = feet.getRelative(0,-1,0);
			if (temp.getType() == Material.AIR) {
				tombBlockType.setBlock(temp);
				tombBlocks.add(temp);
			}
			temp = feet.getRelative(0,2,0);
			if (temp.getType() == Material.AIR) {
				tombBlockType.setBlock(temp);
				tombBlocks.add(temp);
			}
		}				
		
		if (tombDuration > 0 && tombBlocks.size() > 0) {
			blocks.addAll(tombBlocks);
			MagicSpells.plugin.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new TombRemover(tombBlocks), Math.round(tombDuration*20*power));
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			createTomb(target, power);
			playSpellEffects(caster, target);
			return true;
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) {
			return false;
		} else {
			createTomb(target, power);
			playSpellEffects(EffectPosition.TARGET, target);
			return true;
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (blocks.contains(event.getBlock())) {
			event.setCancelled(true);
			if (allowBreaking) {
				event.getBlock().setType(Material.AIR);
			}
		}
	}

	private class TombRemover implements Runnable {

		ArrayList<Block> tomb;
		
		public TombRemover(ArrayList<Block> tomb) {
			this.tomb = tomb;
		}
		
		@Override
		public void run() {
			for (Block block : tomb) {
				if (tombBlockType.getMaterial() == block.getType()) {
					block.setType(Material.AIR);
				}
			}
			blocks.removeAll(tomb);
		}
		
	}

}
