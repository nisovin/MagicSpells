package com.nisovin.magicspells.util;

import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class CraftBukkitHandleDisabled implements CraftBukkitHandle {

	@Override
	public void playPotionEffect(Player player, Entity entity, int color, int duration) {
	}

	@Override
	public void entityPathTo(LivingEntity entity, LivingEntity target) {
	}

	@Override
	public void queueChunksForUpdate(Player player, Set<Chunk> chunks) {
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
	}

	@Override
	public void stackByData(int itemId, String var) {
	}

	@Override
	public void toggleLeverOrButton(Block block) {
		if (block.getType() == Material.STONE_BUTTON) {
			block.setData((byte) (block.getData() ^ 0x1));
		} else {
			byte data = block.getData();
			byte var1 = (byte) (data & 7);
			byte var2 = (byte) (8 - (data & 8));
			block.setData((byte) (var1 + var2));
		}
	}

	@Override
	public void pressPressurePlate(Block block) {
		block.setData((byte) (block.getData() ^ 0x1));
	}

	@Override
	public void removeMobEffect(LivingEntity entity, PotionEffectType type) {
		entity.removePotionEffect(type);
	}

}
