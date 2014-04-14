package com.nisovin.magicspells.volatilecode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.v1_7_R1.*;
import net.minecraft.util.com.google.common.base.Charsets;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelOutboundHandlerAdapter;
import net.minecraft.util.io.netty.channel.ChannelPromise;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell;
import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ReflectionHelper;

public class DisguiseManager_1_7_R1_Injected extends DisguiseManager {

	ReflectionHelper<Packet> refPacketNamedEntity = new ReflectionHelper<Packet>(PacketPlayOutNamedEntitySpawn.class, "a", "b");
	ReflectionHelper<Packet> refPacketSpawnEntityLiving = new ReflectionHelper<Packet>(PacketPlayOutSpawnEntityLiving.class, "a", "i", "j", "k");
	ReflectionHelper<Packet> refPacketSpawnEntity = new ReflectionHelper<Packet>(PacketPlayOutSpawnEntity.class, "a");
	ReflectionHelper<Packet> refPacketEntityEquipment = new ReflectionHelper<Packet>(PacketPlayOutEntityEquipment.class, "a", "b");
	ReflectionHelper<Packet> refPacketRelEntityMove = new ReflectionHelper<Packet>(PacketPlayOutEntity.class, "a", "b", "c", "d");
	ReflectionHelper<Packet> refPacketRelEntityMoveLook = new ReflectionHelper<Packet>(PacketPlayOutEntity.class, "a", "b", "c", "d", "e", "f");
	ReflectionHelper<Packet> refPacketRelEntityTeleport = new ReflectionHelper<Packet>(PacketPlayOutEntityTeleport.class, "a", "b", "c", "d", "e", "f");
	ReflectionHelper<Packet> refPacketEntityLook = new ReflectionHelper<Packet>(PacketPlayOutEntity.class, "a", "e", "f");
	ReflectionHelper<Packet> refPacketEntityHeadRot = new ReflectionHelper<Packet>(PacketPlayOutEntityHeadRotation.class, "a", "b");
	ReflectionHelper<Packet> refPacketEntityMetadata = new ReflectionHelper<Packet>(PacketPlayOutEntityMetadata.class, "a");
	ReflectionHelper<Packet> refPacketAttachEntity = new ReflectionHelper<Packet>(PacketPlayOutAttachEntity.class, "b", "c");
	ReflectionHelper<Entity> refEntity = new ReflectionHelper<Entity>(Entity.class, "id");
	
	BackupPacketListener backupListener;
	
	public DisguiseManager_1_7_R1_Injected(MagicConfig config) {
		super(config);
		backupListener = new BackupPacketListener();
	}
	
	@Override
	protected void cleanup() {
		backupListener.destroy();
	}

	private GameProfile getGameProfile(UUID uuid, String name) {
		try {
			return GameProfile.class.getDeclaredConstructor(String.class, String.class).newInstance(uuid.toString().replaceAll("-", ""), name);
		} catch (Exception e) {
			return null;
		}
	}

	private Entity getEntity(Player player, DisguiseSpell.Disguise disguise) {
		EntityType entityType = disguise.getEntityType();
		boolean flag = disguise.getFlag();
		int var = disguise.getVar1();
		Location location = player.getLocation();
		Entity entity = null;
		float yOffset = 0;
		World world = ((CraftWorld)location.getWorld()).getHandle();
		if (entityType == EntityType.PLAYER) {
			String name = disguise.getNameplateText();
			UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
			entity = new EntityHuman(world, getGameProfile(uuid, name)) {
				@Override
				public boolean a(int arg0, String arg1) {
					return false;
				}
				@Override
				public ChunkCoordinates getChunkCoordinates() {
					return null;
				}
				@Override
				public void sendMessage(IChatBaseComponent arg0) {
				}
			};
			yOffset = -1.5F;
		} else if (entityType == EntityType.ZOMBIE) {
			entity = new EntityZombie(world);
			if (flag) {
				((EntityZombie)entity).setBaby(true);
			}
			if (var == 1) {
				((EntityZombie)entity).setVillager(true);
			}
			
		} else if (entityType == EntityType.SKELETON) {
			entity = new EntitySkeleton(world);
			if (flag) {
				((EntitySkeleton)entity).setSkeletonType(1);
			}
			
		} else if (entityType == EntityType.IRON_GOLEM) {
			entity = new EntityIronGolem(world);
			
		} else if (entityType == EntityType.SNOWMAN) {
			entity = new EntitySnowman(world);
			
		} else if (entityType == EntityType.CREEPER) {
			entity = new EntityCreeper(world);
			if (flag) {
				((EntityCreeper)entity).setPowered(true);
			}
			
		} else if (entityType == EntityType.SPIDER) {
			entity = new EntitySpider(world);
			
		} else if (entityType == EntityType.CAVE_SPIDER) {
			entity = new EntityCaveSpider(world);
			
		} else if (entityType == EntityType.WOLF) {
			entity = new EntityWolf(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			if (var > 0) {
				((EntityWolf)entity).setTamed(true);
				((EntityWolf)entity).setOwnerName(player.getName());
				((EntityWolf)entity).setCollarColor(var);
			}
			
		} else if (entityType == EntityType.OCELOT) {
			entity = new EntityOcelot(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			if (var == -1) {
				((EntityOcelot)entity).setCatType(random.nextInt(4));
			} else if (var >= 0 && var < 4) {
				((EntityOcelot)entity).setCatType(var);
			}
			
		} else if (entityType == EntityType.BLAZE) {
			entity = new EntityBlaze(world);
			
		} else if (entityType == EntityType.GIANT) {
			entity = new EntityGiantZombie(world);
			
		} else if (entityType == EntityType.ENDERMAN) {
			entity = new EntityEnderman(world);
			
		} else if (entityType == EntityType.SILVERFISH) {
			entity = new EntitySilverfish(world);
			
		} else if (entityType == EntityType.WITCH) {
			entity = new EntityWitch(world);
			
		} else if (entityType == EntityType.VILLAGER) {
			entity = new EntityVillager(world);
			
			((EntityVillager)entity).setProfession(var);
		} else if (entityType == EntityType.PIG_ZOMBIE) {
			entity = new EntityPigZombie(world);
			
		} else if (entityType == EntityType.SLIME) {
			entity = new EntitySlime(world);
			entity.getDataWatcher().watch(16, Byte.valueOf((byte)2));
			
		} else if (entityType == EntityType.MAGMA_CUBE) {
			entity = new EntityMagmaCube(world);
			entity.getDataWatcher().watch(16, Byte.valueOf((byte)2));
			
		} else if (entityType == EntityType.BAT) {
			entity = new EntityBat(world);
			entity.getDataWatcher().watch(16, Byte.valueOf((byte)0));
			
		} else if (entityType == EntityType.CHICKEN) {
			entity = new EntityChicken(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			
		} else if (entityType == EntityType.COW) {
			entity = new EntityCow(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			
		} else if (entityType == EntityType.MUSHROOM_COW) {
			entity = new EntityMushroomCow(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			
		} else if (entityType == EntityType.PIG) {
			entity = new EntityPig(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			if (var == 1) {
				((EntityPig)entity).setSaddle(true);
			}
			
		} else if (entityType == EntityType.SHEEP) {
			entity = new EntitySheep(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			if (var == -1) {
				((EntitySheep)entity).setColor(random.nextInt(16));
			} else if (var >= 0 && var < 16) {
				((EntitySheep)entity).setColor(var);
			}
			
		} else if (entityType == EntityType.SQUID) {
			entity = new EntitySquid(world);
			
		} else if (entityType == EntityType.GHAST) {
			entity = new EntityGhast(world);
			
		} else if (entityType == EntityType.WITHER) {
			entity = new EntityWither(world);
			
		} else if (entityType == EntityType.HORSE) {
			entity = new EntityHorse(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			((EntityHorse)entity).getDataWatcher().watch(19, Byte.valueOf((byte)disguise.getVar1()));
			((EntityHorse)entity).getDataWatcher().watch(20, Integer.valueOf(disguise.getVar2()));
			if (disguise.getVar3() > 0) {
				((EntityHorse)entity).getDataWatcher().watch(22, Integer.valueOf(disguise.getVar3()));
			}
			
		} else if (entityType == EntityType.ENDER_DRAGON) {
			entity = new EntityEnderDragon(world);
						
		} else if (entityType == EntityType.FALLING_BLOCK) {
			int id = disguise.getVar1();
			int data = disguise.getVar2();
			entity = new EntityFallingBlock(world, 0, 0, 0, Block.e(id > 0 ? id : 1), data > 15 ? 0 : data);
			
		} else if (entityType == EntityType.DROPPED_ITEM) {
			int id = disguise.getVar1();
			int data = disguise.getVar2();
			entity = new EntityItem(world);
			((EntityItem)entity).setItemStack(new net.minecraft.server.v1_7_R1.ItemStack(Item.d(id > 0 ? id : 1), 1, data));
			
		}
		
		if (entity != null) {
			
			String nameplateText = disguise.getNameplateText();
			if (entity instanceof EntityInsentient && nameplateText != null && !nameplateText.isEmpty()) {
				((EntityInsentient)entity).setCustomName(nameplateText);
				((EntityInsentient)entity).setCustomNameVisible(disguise.alwaysShowNameplate());
			}
			
			entity.setPositionRotation(location.getX(), location.getY() + yOffset, location.getZ(), location.getYaw(), location.getPitch());
			
			return entity;
			
		} else {
			return null;
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onArmSwing(PlayerAnimationEvent event) {
		final Player p = event.getPlayer();
		final int entityId = p.getEntityId();
		if (isDisguised(p)) {
			DisguiseSpell.Disguise disguise = getDisguise(p);
			EntityType entityType = disguise.getEntityType();
			EntityPlayer entityPlayer = ((CraftPlayer)p).getHandle();
			if (entityType == EntityType.IRON_GOLEM) {
				((CraftWorld)p.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity)p).getHandle(), (byte) 4);
			} else if (entityType == EntityType.WITCH) {
				((CraftWorld)p.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity)p).getHandle(), (byte) 15);
			} else if (entityType == EntityType.VILLAGER) {
				((CraftWorld)p.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity)p).getHandle(), (byte) 13);
			} else if (entityType == EntityType.BLAZE || entityType == EntityType.SPIDER || entityType == EntityType.GHAST) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)0));
						broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.WITCH) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(21, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(21, Byte.valueOf((byte)0));
						broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.CREEPER && !disguise.getFlag()) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(17, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(17, Byte.valueOf((byte)0));
						broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.WOLF) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)(p.isSneaking() ? 3 : 2)));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)(p.isSneaking() ? 1 : 0)));
						broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)(p.isSneaking() ? 2 : 3)));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)(p.isSneaking() ? 1 : 2)));
						broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onSneak(PlayerToggleSneakEvent event) {
		DisguiseSpell.Disguise disguise = getDisguise(event.getPlayer());
		if (disguise == null) return;
		EntityType entityType = disguise.getEntityType();
		EntityPlayer entityPlayer = ((CraftPlayer)event.getPlayer()).getHandle();
		Player p = event.getPlayer();
		int entityId = p.getEntityId();
		if (entityType == EntityType.WOLF) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)0));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.ENDERMAN) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(18, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(18, Byte.valueOf((byte)0));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)2));
				broadcastPacket(p, 40, new PacketPlayOutEntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.SHEEP && event.isSneaking()) {
			p.playEffect(EntityEffect.SHEEP_EAT);
		}
	}
	
	class BackupPacketListener implements Listener {

		private Field channelField;
		private Map<Packet, Long> ignoredPackets = new HashMap<Packet, Long>();
		private BukkitTask ignoredPacketsCleanTask;
		
		public BackupPacketListener() {
			try {
				channelField = NetworkManager.class.getDeclaredField("k");
				channelField.setAccessible(true);
				Bukkit.getPluginManager().registerEvents(this, MagicSpells.getInstance());
				ignoredPacketsCleanTask = Bukkit.getScheduler().runTaskTimer(MagicSpells.getInstance(), new Runnable() {
					public void run() {
						synchronized (ignoredPackets) {
							Iterator<Map.Entry<Packet, Long>> iter = ignoredPackets.entrySet().iterator();
							while (iter.hasNext()) {
								if (iter.next().getValue() < System.currentTimeMillis() - 30000) {
									iter.remove();
								}
							}
						}
					}
				}, 30*20, 30*20);
				for (Player player : Bukkit.getOnlinePlayers()) {
					hookPlayer(player);
				}
			} catch (Exception e) {
				MagicSpells.error("Unable to create packet listener for disguise spell!");
				e.printStackTrace();
			}
		}
		
		@EventHandler
		public void onJoin(PlayerJoinEvent event) {
			hookPlayer(event.getPlayer());
		}
		
		private void hookPlayer(Player player) {
			try {
				Channel channel = (Channel) this.channelField.get(((CraftPlayer) player).getHandle().playerConnection.networkManager);
				channel.pipeline().addLast("MagicSpells_Disguises", new PacketHandler(player));
			} catch (Exception e) {
				MagicSpells.error("Failed to hook player " + player.getName() + " for disguise spells");
				e.printStackTrace();
			}
		}
		
		public void ignorePacket(Packet packet) {
			synchronized (ignoredPackets) {
				ignoredPackets.put(packet, System.currentTimeMillis());
			}
		}
		
		public boolean isPacketIgnored(Packet packet) {
			synchronized (ignoredPackets) {
				return ignoredPackets.containsKey(packet);
			}
		}
		
		public void destroy() {
			HandlerList.unregisterAll(this);
			ignoredPacketsCleanTask.cancel();
			ignoredPackets.clear();
			for (Player player : Bukkit.getOnlinePlayers()) {
				try {
					Channel channel = (Channel) this.channelField.get(((CraftPlayer) player).getHandle().playerConnection.networkManager);
					channel.pipeline().remove("MagicSpells_Disguises");
				} catch (Exception e) {
					MagicSpells.error("Failed to unhook player " + player.getName() + " for disguise spells");
					e.printStackTrace();
				}
			}
		}
		
		class PacketHandler extends ChannelOutboundHandlerAdapter {
			
			Player player;
			
			public PacketHandler(Player player) {
				this.player = player;
			}
			
			@Override
			public void write(ChannelHandlerContext ctx, Object packetObject, ChannelPromise promise) throws Exception {
				Packet packet = (Packet)packetObject;
				if (!isPacketIgnored(packet)) {
					if (packet instanceof PacketPlayOutNamedEntitySpawn) {
						final String name = ((GameProfile)refPacketNamedEntity.get(packet, "b")).getName();
						final DisguiseSpell.Disguise disguise = disguises.get(name.toLowerCase());
						if (player != null && disguise != null) {
							Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
								public void run() {
									Player disguised = Bukkit.getPlayer(name);
									if (disguised != null) {
										sendDisguisedSpawnPacket(player, disguised, disguise, null);
									}
								}
							}, 0);
							return;
						}
					} else if (hideArmor && packet instanceof PacketPlayOutEntityEquipment) {
						if (refPacketEntityEquipment.getInt(packet, "b") > 0 && disguisedEntityIds.containsKey(refPacketEntityEquipment.getInt(packet, "a"))) {
							return;
						}
					} else if (packet instanceof PacketPlayOutRelEntityMove) {
						int entId = refPacketRelEntityMove.getInt(packet, "a");
						if (mounts.containsKey(entId)) {
							PacketPlayOutRelEntityMove newpacket = new PacketPlayOutRelEntityMove(entId, refPacketRelEntityMove.getByte(packet, "b"), refPacketRelEntityMove.getByte(packet, "c"), refPacketRelEntityMove.getByte(packet, "d"));
							ignorePacket(newpacket);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
						}
					} else if (packet instanceof PacketPlayOutEntityMetadata) {
						int entId = refPacketEntityMetadata.getInt(packet, "a");
						DisguiseSpell.Disguise disguise = disguisedEntityIds.get(entId);
						if (disguise != null && disguise.getEntityType() != EntityType.PLAYER) {
							return;
						}
					} else if (packet instanceof PacketPlayOutRelEntityMoveLook) {
						int entId = refPacketRelEntityMoveLook.getInt(packet, "a");
						if (dragons.contains(entId)) {
							int dir = refPacketRelEntityMoveLook.getByte(packet, "e") + 128;
							if (dir > 127) dir -= 256;
							PacketPlayOutRelEntityMoveLook newpacket = new PacketPlayOutRelEntityMoveLook(
									entId,
									refPacketRelEntityMoveLook.getByte(packet, "b"),
									refPacketRelEntityMoveLook.getByte(packet, "c"),
									refPacketRelEntityMoveLook.getByte(packet, "d"),
									(byte)dir,
									refPacketRelEntityMoveLook.getByte(packet, "f"));
							ignorePacket(newpacket);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
							PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(entId, 0.15, 0, 0.15);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet28);
							return;
						} else if (mounts.containsKey(entId)) {
							PacketPlayOutRelEntityMoveLook newpacket = new PacketPlayOutRelEntityMoveLook(
									mounts.get(entId),
									refPacketRelEntityMoveLook.getByte(packet, "b"),
									refPacketRelEntityMoveLook.getByte(packet, "c"),
									refPacketRelEntityMoveLook.getByte(packet, "d"),
									refPacketRelEntityMoveLook.getByte(packet, "e"),
									refPacketRelEntityMoveLook.getByte(packet, "f"));
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
						}
					} else if (packet instanceof PacketPlayOutEntityLook) {
						int entId = refPacketEntityLook.getInt(packet, "a");
						if (dragons.contains(entId)) {
							int dir = refPacketEntityLook.getByte(packet, "e") + 128;
							if (dir > 127) dir -= 256;
							PacketPlayOutEntityLook newpacket = new PacketPlayOutEntityLook(
									entId,
									(byte)dir,
									refPacketRelEntityMoveLook.getByte(packet, "f"));
							ignorePacket(newpacket);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
							PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(entId, 0.15, 0, 0.15);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet28);
							return;
						} else if (mounts.containsKey(entId)) {
							PacketPlayOutRelEntityMoveLook newpacket = new PacketPlayOutRelEntityMoveLook(
									mounts.get(entId),
									refPacketRelEntityMoveLook.getByte(packet, "b"),
									refPacketRelEntityMoveLook.getByte(packet, "c"),
									refPacketRelEntityMoveLook.getByte(packet, "d"),
									refPacketRelEntityMoveLook.getByte(packet, "e"),
									refPacketRelEntityMoveLook.getByte(packet, "f"));
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
						}
					} else if (packet instanceof PacketPlayOutEntityTeleport) {
						int entId = refPacketRelEntityTeleport.getInt(packet, "a");
						if (dragons.contains(entId)) {
							int dir = refPacketRelEntityTeleport.getByte(packet, "e") + 128;
							if (dir > 127) dir -= 256;
							PacketPlayOutEntityTeleport newpacket = new PacketPlayOutEntityTeleport(
									entId,
									refPacketRelEntityTeleport.getInt(packet, "b"),
									refPacketRelEntityTeleport.getInt(packet, "c"),
									refPacketRelEntityTeleport.getInt(packet, "d"),
									(byte)dir,
									refPacketRelEntityTeleport.getByte(packet, "f"));
							ignorePacket(newpacket);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
							PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(entId, 0.15, 0, 0.15);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet28);
							return;
						} else if (mounts.containsKey(entId)) {
							PacketPlayOutEntityTeleport newpacket = new PacketPlayOutEntityTeleport(
									mounts.get(entId),
									refPacketRelEntityTeleport.getInt(packet, "b"),
									refPacketRelEntityTeleport.getInt(packet, "c"),
									refPacketRelEntityTeleport.getInt(packet, "d"),
									refPacketRelEntityTeleport.getByte(packet, "e"),
									refPacketRelEntityTeleport.getByte(packet, "f"));
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
							PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(entId, 0.15, 0, 0.15);
							((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet28);
						}
					} else if (packet instanceof PacketPlayOutEntityHeadRotation) {
						int entId = refPacketEntityHeadRot.getInt(packet, "a");
						if (dragons.contains(entId)) {
							return;
						}
					}
				}
				super.write(ctx, packet, promise);
			}
		}
		
	}
	
	@Override
	protected void sendDestroyEntityPackets(Player disguised) {
		sendDestroyEntityPackets(disguised, disguised.getEntityId());
	}
	
	@Override
	protected void sendDestroyEntityPackets(Player disguised, int entityId) {
		PacketPlayOutEntityDestroy packet29 = new PacketPlayOutEntityDestroy(entityId);
		final EntityTracker tracker = ((CraftWorld)disguised.getWorld()).getHandle().tracker;
		tracker.a(((CraftPlayer)disguised).getHandle(), packet29);
	}
	
	private void broadcastPacket(Player disguised, int packetId, Packet packet) {
		backupListener.ignorePacket(packet);
		((EntityTrackerEntry)((CraftWorld)disguised.getWorld()).getHandle().tracker.trackedEntities.get(disguised.getEntityId())).broadcast(packet);
	}
	
	private void sendDisguisedSpawnPacket(Player viewer, Player disguised, DisguiseSpell.Disguise disguise, Entity entity) {
		if (entity == null) entity = getEntity(disguised, disguise);
		if (entity != null) {
			List<Packet> packets = getPacketsToSend(disguised, disguise, entity);
			if (packets != null && packets.size() > 0) {
				EntityPlayer ep = ((CraftPlayer)viewer).getHandle();
				try {
					for (Packet packet : packets) {
						backupListener.ignorePacket(packet);
						ep.playerConnection.sendPacket(packet);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	protected void sendDisguisedSpawnPackets(Player disguised, DisguiseSpell.Disguise disguise) {
		Entity entity = getEntity(disguised, disguise);
		if (entity != null) {
			List<Packet> packets = getPacketsToSend(disguised, disguise, entity);
			if (packets != null && packets.size() > 0) {
				final EntityTracker tracker = ((CraftWorld)disguised.getWorld()).getHandle().tracker;
				for (Packet packet : packets) {
					if (packet instanceof PacketPlayOutEntityMetadata) {
						broadcastPacket(disguised, 40, packet);
					} else if (packet instanceof PacketPlayOutEntityMetadata) {
						broadcastPacket(disguised, 20, packet);
					} else {
						tracker.a(((CraftPlayer)disguised).getHandle(), packet);
					}
				}
			}
		}
	}
	
	private List<Packet> getPacketsToSend(Player disguised, DisguiseSpell.Disguise disguise, Entity entity) {
		List<Packet> packets = new ArrayList<Packet>();
		if (entity instanceof EntityHuman) {
			PacketPlayOutNamedEntitySpawn packet20 = new PacketPlayOutNamedEntitySpawn((EntityHuman)entity);
			refPacketNamedEntity.setInt(packet20, "a", disguised.getEntityId());
			packets.add(packet20);
			
			ItemStack inHand = disguised.getItemInHand();
			if (inHand != null && inHand.getType() != Material.AIR) {
				PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), 0, CraftItemStack.asNMSCopy(inHand));
				packets.add(packet5);
			}
			
			ItemStack helmet = disguised.getInventory().getHelmet();
			if (helmet != null && helmet.getType() != Material.AIR) {
				PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), 4, CraftItemStack.asNMSCopy(helmet));
				packets.add(packet5);
			}
			
			ItemStack chestplate = disguised.getInventory().getChestplate();
			if (chestplate != null && chestplate.getType() != Material.AIR) {
				PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), 3, CraftItemStack.asNMSCopy(chestplate));
				packets.add(packet5);
			}
			
			ItemStack leggings = disguised.getInventory().getLeggings();
			if (leggings != null && leggings.getType() != Material.AIR) {
				PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), 2, CraftItemStack.asNMSCopy(leggings));
				packets.add(packet5);
			}
			
			ItemStack boots = disguised.getInventory().getBoots();
			if (boots != null && boots.getType() != Material.AIR) {
				PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), 1, CraftItemStack.asNMSCopy(boots));
				packets.add(packet5);
			}
		} else if (entity instanceof EntityLiving) {
			PacketPlayOutSpawnEntityLiving packet24 = new PacketPlayOutSpawnEntityLiving((EntityLiving)entity);
			refPacketSpawnEntityLiving.setInt(packet24, "a", disguised.getEntityId());
			if (dragons.contains(disguised.getEntityId())) {
				int dir = refPacketSpawnEntityLiving.getByte(packet24, "i") + 128;
				if (dir > 127) dir -= 256;
				refPacketSpawnEntityLiving.setByte(packet24, "i", (byte)dir);
				refPacketSpawnEntityLiving.setByte(packet24, "j", (byte)0);
				refPacketSpawnEntityLiving.setByte(packet24, "k", (byte)1);
			}
			packets.add(packet24);
			PacketPlayOutEntityMetadata packet40 = new PacketPlayOutEntityMetadata(disguised.getEntityId(), entity.getDataWatcher(), false);
			packets.add(packet40);
			if (dragons.contains(disguised.getEntityId())) {
				PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(disguised.getEntityId(), 0.15, 0, 0.15);
				packets.add(packet28);
			}
			
			if (disguise.getEntityType() == EntityType.ZOMBIE || disguise.getEntityType() == EntityType.SKELETON) {
				ItemStack inHand = disguised.getItemInHand();
				if (inHand != null && inHand.getType() != Material.AIR) {
					PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), 0, CraftItemStack.asNMSCopy(inHand));
					packets.add(packet5);
				}
			}
		} else if (entity instanceof EntityFallingBlock) {
			PacketPlayOutSpawnEntity packet23 = new PacketPlayOutSpawnEntity(entity, 70, disguise.getVar1() | ((byte)disguise.getVar2()) << 16);
			refPacketSpawnEntity.setInt(packet23, "a", disguised.getEntityId());
			packets.add(packet23);
		} else if (entity instanceof EntityItem) {
			PacketPlayOutSpawnEntity packet23 = new PacketPlayOutSpawnEntity(entity, 2, 1);
			refPacketSpawnEntity.setInt(packet23, "a", disguised.getEntityId());
			packets.add(packet23);
			PacketPlayOutEntityMetadata packet40 = new PacketPlayOutEntityMetadata(disguised.getEntityId(), entity.getDataWatcher(), true);
			packets.add(packet40);
		}
		
		if (disguise.isRidingBoat()) {
			EntityBoat boat = new EntityBoat(entity.world);
			int boatEntId;
			if (mounts.containsKey(disguised.getEntityId())) {
				boatEntId = mounts.get(disguised.getEntityId());
				refEntity.setInt(boat, "id", boatEntId);
			} else {
				boatEntId = refEntity.getInt(boat, "id");
				mounts.put(disguised.getEntityId(), boatEntId);
			}
			boat.setPositionRotation(disguised.getLocation().getX(), disguised.getLocation().getY(), disguised.getLocation().getZ(), disguised.getLocation().getYaw(), 0);
			PacketPlayOutSpawnEntity packet23 = new PacketPlayOutSpawnEntity(boat, 1);
			packets.add(packet23);
			PacketPlayOutAttachEntity packet39 = new PacketPlayOutAttachEntity();
			refPacketAttachEntity.setInt(packet39, "b", disguised.getEntityId());
			refPacketAttachEntity.setInt(packet39, "c", boatEntId);
			packets.add(packet39);
		}
		
		// handle passengers and vehicles
		if (disguised.getPassenger() != null) {
			PacketPlayOutAttachEntity packet39 = new PacketPlayOutAttachEntity();
			refPacketAttachEntity.setInt(packet39, "b", disguised.getPassenger().getEntityId());
			refPacketAttachEntity.setInt(packet39, "c", disguised.getEntityId());
			packets.add(packet39);
		}
		if (disguised.getVehicle() != null) {
			PacketPlayOutAttachEntity packet39 = new PacketPlayOutAttachEntity();
			refPacketAttachEntity.setInt(packet39, "b", disguised.getEntityId());
			refPacketAttachEntity.setInt(packet39, "c", disguised.getVehicle().getEntityId());
			packets.add(packet39);
		}
		
		return packets;
	}
	
	@Override
	protected void sendPlayerSpawnPackets(Player player) {
		PacketPlayOutNamedEntitySpawn packet20 = new PacketPlayOutNamedEntitySpawn(((CraftPlayer)player).getHandle());
		final EntityTracker tracker = ((CraftWorld)player.getWorld()).getHandle().tracker;
		tracker.a(((CraftPlayer)player).getHandle(), packet20);
	}
}