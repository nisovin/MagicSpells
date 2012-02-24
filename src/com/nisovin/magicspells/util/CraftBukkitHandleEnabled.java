package com.nisovin.magicspells.util;

import java.lang.reflect.Field;
import java.util.Set;

import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.DataWatcher;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MobEffect;
import net.minecraft.server.Packet103SetSlot;
import net.minecraft.server.Packet40EntityMetadata;
import net.minecraft.server.Packet42RemoveMobEffect;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftCreature;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.MagicSpells;

public class CraftBukkitHandleEnabled implements CraftBukkitHandle {

	@Override
	public void playPotionEffect(final Player player, final Entity entity, int color, int duration) {
		final DataWatcher dw = new DataWatcher();
		dw.a(8, Integer.valueOf(0));
		dw.watch(8, Integer.valueOf(color));
		
		Packet40EntityMetadata packet = new Packet40EntityMetadata(entity.getEntityId(), dw);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				DataWatcher dwReal = ((CraftLivingEntity)entity).getHandle().getDataWatcher();
				dw.watch(8, dwReal.getInt(8));
				Packet40EntityMetadata packet = new Packet40EntityMetadata(entity.getEntityId(), dw);
				((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
			}
		}, duration);
	}

	@Override
	public void entityPathTo(LivingEntity creature, LivingEntity target) {
		EntityCreature entity = ((CraftCreature)creature).getHandle();
		entity.pathEntity = entity.world.findPath(entity, ((CraftLivingEntity)target).getHandle(), 16.0F);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void queueChunksForUpdate(Player player, Set<Chunk> chunks) {
		for (Chunk chunk : chunks) {
			ChunkCoordIntPair intPair = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
			((CraftPlayer)player).getHandle().chunkCoordIntPairQueue.add(intPair);
		}
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		net.minecraft.server.ItemStack nmsItem = null;
		if (item != null) {
			nmsItem = new net.minecraft.server.ItemStack(item.getTypeId(), slot+1, item.getDurability());
		} else {
			nmsItem = null;
		}
		Packet103SetSlot packet = new Packet103SetSlot(0, (short)slot+36, nmsItem);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
	}

	@Override
	public void stackByData(int itemId, String var) {
		try {
			boolean ok = false;
			try {
				// attempt to make books with different data values stack separately
				Field field1 = net.minecraft.server.Item.class.getDeclaredField(var);
				if (field1.getType() == boolean.class) {
					field1.setAccessible(true);
					field1.setBoolean(net.minecraft.server.Item.byId[itemId], true);
					ok = true;
				} 
			} catch (Exception e) {
			}
			if (!ok) {
				// otherwise limit stack size to 1
				Field field2 = net.minecraft.server.Item.class.getDeclaredField("maxStackSize");
				field2.setAccessible(true);
				field2.setInt(net.minecraft.server.Item.byId[itemId], 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toggleLeverOrButton(Block block) {
		net.minecraft.server.Block.byId[block.getType().getId()].interact(((CraftWorld)block.getWorld()).getHandle(), block.getX(), block.getY(), block.getZ(), null);
	}

	@Override
	public void pressPressurePlate(Block block) {
		block.setData((byte) (block.getData() ^ 0x1));
		net.minecraft.server.World w = ((CraftWorld)block.getWorld()).getHandle();
		w.applyPhysics(block.getX(), block.getY(), block.getZ(), block.getType().getId());
		w.applyPhysics(block.getX(), block.getY()-1, block.getZ(), block.getType().getId());
	}

	@Override
	public void removeMobEffect(LivingEntity entity, PotionEffectType type) {
        try {
            // remove effect
    		entity.removePotionEffect(type);
            // alert player that effect is gone
            if (entity instanceof Player) {
                    EntityPlayer player = ((CraftPlayer)entity).getHandle();
                    player.netServerHandler.sendPacket(new Packet42RemoveMobEffect(player.id, new MobEffect(type.getId(), 0, 0)));
            }
            // remove graphical effect
            ((CraftLivingEntity)entity).getHandle().getDataWatcher().watch(8, Integer.valueOf(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
