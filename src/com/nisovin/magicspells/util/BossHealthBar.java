package com.nisovin.magicspells.util;

import net.minecraft.server.DataWatcher;
import net.minecraft.server.EntityEnderDragon;
import net.minecraft.server.Packet24MobSpawn;
import net.minecraft.server.Packet29DestroyEntity;
import net.minecraft.server.Packet40EntityMetadata;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BossHealthBar {

	private Player player = null;
	private int dragonId = -1;
	private DataWatcher dw = null;
	
	public BossHealthBar(Player player) {
		this.player = player;
	}
	
	public void create() {
		EntityEnderDragon dragon = new EntityEnderDragon(((CraftWorld)player.getWorld()).getHandle());
		dragon.setLocation(player.getLocation().getX(), -50, player.getLocation().getZ(), 0, 0);
		dragonId = dragon.id;
		dw = dragon.getDataWatcher();
		dw.watch(16, Integer.valueOf(200));
		
		Packet24MobSpawn packet = new Packet24MobSpawn(dragon);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
	}
	
	public void update(LivingEntity entity) {
		update((float)entity.getHealth() / (float)entity.getMaxHealth());
	}
	
	public void update(int health, int max) {
		update((float)health / (float)max);
	}
	
	public void update(float percent) {
		if (percent <= 0) {
			disable();
		} else {
			if (dw == null || dragonId < 0) {
				create();
			}
			dw.watch(16, Integer.valueOf(Math.round(percent * 200)));		
			Packet40EntityMetadata packet = new Packet40EntityMetadata(dragonId, dw);
			((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
		}
	}
	
	public void disable() {
		if (dragonId > 0) {
			Packet29DestroyEntity packet = new Packet29DestroyEntity(dragonId);
			((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
		}
		dw = null;
		dragonId = -1;
	}
	
}
