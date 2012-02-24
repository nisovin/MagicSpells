package com.nisovin.magicspells.util;

import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public interface CraftBukkitHandle {

	public void playPotionEffect(Player player, Entity entity, int color, int duration);
	
	public void entityPathTo(LivingEntity entity, LivingEntity target);
	
	public void queueChunksForUpdate(Player player, Set<Chunk> chunks);
	
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item);
	
	public void stackByData(int itemId, String var);
	
	public void toggleLeverOrButton(Block block);
	
	public void pressPressurePlate(Block block);
	
	public void removeMobEffect(LivingEntity entity, PotionEffectType type);
	
}
