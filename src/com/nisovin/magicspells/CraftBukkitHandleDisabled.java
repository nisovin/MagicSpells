package com.nisovin.magicspells;

import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

class CraftBukkitHandleDisabled implements CraftBukkitHandle {

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
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
		entity.addPotionEffect(new PotionEffect(type, 1, 0), true);
		entity.removePotionEffect(type);
	}

	@Override
	public void collectItem(Player player, Item item) {
	}

	@Override
	public boolean simulateTnt(Location target, float explosionSize, boolean fire) {
		return false;
	}

	@Override
	public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire) {
		return location.getWorld().createExplosion(location, size, fire);
	}

	@Override
	public void setExperienceBar(Player player, int level, float percent) {
		player.setLevel(level);
		player.setExp(percent);
	}

}
