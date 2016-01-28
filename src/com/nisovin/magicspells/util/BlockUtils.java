package com.nisovin.magicspells.util;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;

public class BlockUtils {

	public static boolean isTransparent(Spell spell, Block block) {
		return spell.getLosTransparentBlocks().contains((byte)block.getTypeId());
	}
	
	public static Block getTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			if (spell != null) {
				return entity.getTargetBlock(spell.getLosTransparentBlocks(), range);
			} else {
				return entity.getTargetBlock(MagicSpells.getTransparentBlocks(), range);				
			}
		} catch (IllegalStateException e) {
			return null;
		}
	}
	
	public static List<Block> getLastTwoTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			return entity.getLastTwoTargetBlocks(spell.getLosTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			return null;
		}
	}
	
	public static void setTypeAndData(Block block, Material material, byte data, boolean physics) {
		block.setTypeIdAndData(material.getId(), data, physics);
	}
	
	public static void setBlockFromFallingBlock(Block block, FallingBlock fallingBlock, boolean physics) {
		block.setTypeIdAndData(fallingBlock.getBlockId(), fallingBlock.getBlockData(), physics);
	}
	
	public static int getWaterLevel(Block block) {
		return block.getData();
	}
	
	public static int getGrowthLevel(Block block) {
		return block.getData();
	}
	
	public static void setGrowthLevel(Block block, int level) {
		block.setData((byte)level);
	}
	
	public static int getWaterLevel(BlockState blockState) {
		return blockState.getRawData();
	}
	
	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}
	
	public static boolean isPathable(Material material) {
		return
				material == Material.AIR ||
				material == Material.SAPLING ||
				material == Material.WATER ||
				material == Material.STATIONARY_WATER ||
				material == Material.POWERED_RAIL ||
				material == Material.DETECTOR_RAIL ||
				material == Material.LONG_GRASS ||
				material == Material.DEAD_BUSH ||
				material == Material.YELLOW_FLOWER ||
				material == Material.RED_ROSE ||
				material == Material.BROWN_MUSHROOM ||
				material == Material.RED_MUSHROOM ||
				material == Material.TORCH ||
				material == Material.FIRE ||
				material == Material.REDSTONE_WIRE ||
				material == Material.CROPS ||
				material == Material.SIGN_POST ||
				material == Material.LADDER ||
				material == Material.RAILS ||
				material == Material.WALL_SIGN ||
				material == Material.LEVER ||
				material == Material.STONE_PLATE ||
				material == Material.WOOD_PLATE ||
				material == Material.REDSTONE_TORCH_OFF ||
				material == Material.REDSTONE_TORCH_ON ||
				material == Material.STONE_BUTTON ||
				material == Material.SNOW ||
				material == Material.SUGAR_CANE_BLOCK ||
				material == Material.VINE ||
				material == Material.WATER_LILY ||
				material == Material.NETHER_STALK ||
				material == Material.CARPET;
	}
	
	public static boolean isSafeToStand(Location location) {
		return 
				isPathable(location.getBlock()) && 
				isPathable(location.add(0, 1, 0).getBlock()) && 
				(!isPathable(location.subtract(0, 2, 0).getBlock()) || !isPathable(location.subtract(0, 1, 0).getBlock()));
	}
	
}
