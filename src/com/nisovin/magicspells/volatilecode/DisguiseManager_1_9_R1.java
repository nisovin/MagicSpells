package com.nisovin.magicspells.volatilecode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.v1_9_R1.*;
import net.minecraft.server.v1_9_R1.WorldSettings.*;
import net.minecraft.server.v1_9_R1.PacketPlayOutPlayerInfo.*;
import net.minecraft.server.v1_9_R1.PacketPlayOutEntity.*;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_9_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell.PlayerDisguiseData;
import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ReflectionHelper;

public class DisguiseManager_1_9_R1 extends DisguiseManager {

	ReflectionHelper<Packet> refPacketNamedEntity = new ReflectionHelper<Packet>(PacketPlayOutNamedEntitySpawn.class, "a", "b");
	ReflectionHelper<Packet> refPacketPlayerInfo = new ReflectionHelper<Packet>(PacketPlayOutPlayerInfo.class, "a", "b");
	ReflectionHelper<Packet> refPacketSpawnEntityLiving = new ReflectionHelper<Packet>(PacketPlayOutSpawnEntityLiving.class, "a", "i", "j", "k");
	ReflectionHelper<Packet> refPacketSpawnEntity = new ReflectionHelper<Packet>(PacketPlayOutSpawnEntity.class, "a");
	ReflectionHelper<Packet> refPacketEntityEquipment = new ReflectionHelper<Packet>(PacketPlayOutEntityEquipment.class, "a", "b");
	ReflectionHelper<Packet> refPacketRelEntityMove = new ReflectionHelper<Packet>(PacketPlayOutEntity.class, "a", "b", "c", "d");
	ReflectionHelper<Packet> refPacketRelEntityMoveLook = new ReflectionHelper<Packet>(PacketPlayOutEntity.class, "a", "b", "c", "d", "e", "f");
	ReflectionHelper<Packet> refPacketRelEntityTeleport = new ReflectionHelper<Packet>(PacketPlayOutEntityTeleport.class, "a", "b", "c", "d", "e", "f");
	ReflectionHelper<Packet> refPacketEntityLook = new ReflectionHelper<Packet>(PacketPlayOutEntity.class, "a", "e", "f");
	ReflectionHelper<Packet> refPacketEntityHeadRot = new ReflectionHelper<Packet>(PacketPlayOutEntityHeadRotation.class, "a", "b");
	ReflectionHelper<Packet> refPacketEntityMetadata = new ReflectionHelper<Packet>(PacketPlayOutEntityMetadata.class, "a");
	ReflectionHelper<Packet> refPacketAttachEntity = new ReflectionHelper<Packet>(PacketPlayOutAttachEntity.class, "a", "b");
	ReflectionHelper<Entity> refEntity = new ReflectionHelper<Entity>(Entity.class, "id");
	
	protected ProtocolManager protocolManager;
	protected PacketAdapter packetListener = null;
	
	public DisguiseManager_1_9_R1(MagicConfig config) {
		super(config);
		protocolManager = ProtocolLibrary.getProtocolManager();
		packetListener = new PacketListener();
		protocolManager.addPacketListener(packetListener);
	}
	
	@Override
	protected void cleanup() {
		protocolManager.removePacketListener(packetListener);
	}

	private GameProfile getGameProfile(String name, PlayerDisguiseData data) {
		try {
			UUID uuid = null;
			try {
				if (data != null && data.uuid != null && !data.uuid.isEmpty()) {
					uuid = UUID.fromString(data.uuid);
				}
			} catch (Exception e) {
			}
			
			GameProfile profile = new GameProfile(uuid, name);
			
			if (data != null && data.skin != null && data.sig != null) {
				Property prop = new Property("textures", data.skin, data.sig);
				profile.getProperties().put("textures", prop);
			}
			
			return profile;
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
		String name = disguise.getNameplateText();
		if (name == null || name.isEmpty()) name = player.getName();
		if (entityType == EntityType.PLAYER) {
			entity = new EntityHuman(world, getGameProfile(name, disguise.getPlayerDisguiseData())) {
				@Override
				public boolean a(int arg0, String arg1) {
					return false;
				}
				@Override
				public void sendMessage(IChatBaseComponent arg0) {
				}
				@Override
				public BlockPosition getChunkCoordinates() {
					return null;
				}
				@Override
				public boolean isSpectator() {
					return false;
				}
				@Override
				public boolean l_() {
					return false;
				}
			};
			// TODO: entity.getDataWatcher().watch(10, (Byte)(byte)255);
			yOffset = -1.5F;
		} else if (entityType == EntityType.ZOMBIE) {
			entity = new EntityZombie(world);
			if (flag) {
				((EntityZombie)entity).setBaby(true);
			}
			if (var >= 1) {
				((EntityZombie)entity).setVillagerType(var);
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
				((EntityWolf)entity).setOwnerUUID(player.getUniqueId());
				//((EntityWolf)entity).setCollarColor(var);
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
			if (flag) {
				((EntityVillager)entity).setAge(-24000);
			}
			((EntityVillager)entity).setProfession(var);
			
		} else if (entityType == EntityType.PIG_ZOMBIE) {
			entity = new EntityPigZombie(world);
			if (flag) {
				((EntityPigZombie)entity).setBaby(true);
			}
			
		} else if (entityType == EntityType.SLIME) {
			entity = new EntitySlime(world);
			((EntitySlime)entity).setSize(2);
			
		} else if (entityType == EntityType.MAGMA_CUBE) {
			entity = new EntityMagmaCube(world);
			((EntitySlime)entity).setSize(2);
			
		} else if (entityType == EntityType.BAT) {
			entity = new EntityBat(world);
			((EntityBat)entity).setAsleep(false);
			
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
				((EntitySheep)entity).setColor(EnumColor.fromColorIndex(random.nextInt(16)));
			} else if (var >= 0 && var < 16) {
				((EntitySheep)entity).setColor(EnumColor.fromColorIndex(var));
			}
			
		} else if (entityType == EntityType.SQUID) {
			entity = new EntitySquid(world);
			
		} else if (entityType == EntityType.GHAST) {
			entity = new EntityGhast(world);
			
		} else if (entityType == EntityType.RABBIT) {
			entity = new EntityRabbit(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			((EntityRabbit)entity).setRabbitType(var);
			
		} else if (entityType == EntityType.GUARDIAN) {
			entity = new EntityGuardian(world);
			if (flag) {
				((EntityGuardian)entity).setElder(true);
			}
			
		} else if (entityType == EntityType.ENDERMITE) {
			entity = new EntityEndermite(world);
			
		} else if (entityType == EntityType.WITHER) {
			entity = new EntityWither(world);
			
		} else if (entityType == EntityType.HORSE) {
			entity = new EntityHorse(world);
			((EntityAgeable)entity).setAge(flag ? -24000 : 0);
			// TODO
			//((EntityHorse)entity).getDataWatcher().watch(19, Byte.valueOf((byte)disguise.getVar1()));
			//((EntityHorse)entity).getDataWatcher().watch(20, Integer.valueOf(disguise.getVar2()));
			//if (disguise.getVar3() > 0) {
			//	((EntityHorse)entity).getDataWatcher().watch(22, Integer.valueOf(disguise.getVar3()));
			//}
			
		} else if (entityType == EntityType.ENDER_DRAGON) {
			entity = new EntityEnderDragon(world);
						
		} else if (entityType == EntityType.FALLING_BLOCK) {
			int id = disguise.getVar1();
			int data = disguise.getVar2(); // TODO: fix this
			entity = new EntityFallingBlock(world, 0, 0, 0, Block.getById(id > 0 ? id : 1).getBlockData());
			
		} else if (entityType == EntityType.DROPPED_ITEM) {
			int id = disguise.getVar1();
			int data = disguise.getVar2();
			entity = new EntityItem(world);
			((EntityItem)entity).setItemStack(new net.minecraft.server.v1_9_R1.ItemStack(Item.getById(id > 0 ? id : 1), 1, data));
			
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
				/*final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)0));
						broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);*/
			} else if (entityType == EntityType.WITCH) {
				/*final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(21, Byte.valueOf((byte)1));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(21, Byte.valueOf((byte)0));
						broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);*/
			} else if (entityType == EntityType.WOLF) {
				/*final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)(p.isSneaking() ? 3 : 2)));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)(p.isSneaking() ? 1 : 0)));
						broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);*/
			} else if (entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE) {
				/*final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)(p.isSneaking() ? 2 : 3)));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)(p.isSneaking() ? 1 : 2)));
						broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
					}
				}, 10);*/
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
		/*if (entityType == EntityType.WOLF) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)0));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.ENDERMAN) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(18, Byte.valueOf((byte)1));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(18, Byte.valueOf((byte)0));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher(entityPlayer);
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)2));
				broadcastPacketDisguised(p, PacketType.Play.Server.ENTITY_METADATA, new PacketPlayOutEntityMetadata(entityId, dw, true));
			}
		} else */if (entityType == EntityType.SHEEP && event.isSneaking()) {
			p.playEffect(EntityEffect.SHEEP_EAT);
		}
	}
	
	class PacketListener extends PacketAdapter {
		
		public PacketListener() {
			super(MagicSpells.plugin, ListenerPriority.NORMAL,
					PacketType.Play.Server.NAMED_ENTITY_SPAWN,
					PacketType.Play.Server.PLAYER_INFO,
					PacketType.Play.Server.ENTITY_EQUIPMENT,
					PacketType.Play.Server.REL_ENTITY_MOVE,
					PacketType.Play.Server.ENTITY_MOVE_LOOK,
					PacketType.Play.Server.ENTITY_LOOK,
					PacketType.Play.Server.ENTITY_METADATA,
					PacketType.Play.Server.ENTITY_TELEPORT,
					PacketType.Play.Server.ENTITY_HEAD_ROTATION);
		}
		
		@Override
		public void onPacketSending(PacketEvent event) {
			final Player player = event.getPlayer();
			final PacketContainer packetContainer = event.getPacket();
			final Packet packet = (Packet)packetContainer.getHandle();
			if (packet instanceof PacketPlayOutNamedEntitySpawn) {
				UUID uuid = (UUID)refPacketNamedEntity.get(packet, "b");
				Player p = Bukkit.getPlayer(uuid);
				if (p == null) return;
				final String name = p.getName();
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
					event.setCancelled(true);
				}
			} else if (packet instanceof PacketPlayOutPlayerInfo) {
			} else if (hideArmor && packet instanceof PacketPlayOutEntityEquipment) {
				if (refPacketEntityEquipment.getInt(packet, "b") > 0 && disguisedEntityIds.containsKey(refPacketEntityEquipment.getInt(packet, "a"))) {
					event.setCancelled(true);
				}
			} else if (packet instanceof PacketPlayOutRelEntityMove) {
				int entId = refPacketRelEntityMove.getInt(packet, "a");
				if (mounts.containsKey(entId)) {
					// TODO: FIX: broken in Spigot 1.8 protocol hack
					//PacketPlayOutRelEntityMove newpacket = new PacketPlayOutRelEntityMove(mounts.get(entId), refPacketRelEntityMove.getByte(packet, "b"), refPacketRelEntityMove.getByte(packet, "c"), refPacketRelEntityMove.getByte(packet, "d"));
					//((CraftPlayer)player).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (packet instanceof PacketPlayOutEntityMetadata) {
				int entId = refPacketEntityMetadata.getInt(packet, "a");
				if (event.getPlayer().getEntityId() != entId) {
					DisguiseSpell.Disguise disguise = disguisedEntityIds.get(entId);
					if (disguise != null && disguise.getEntityType() != EntityType.PLAYER) {
						event.setCancelled(true);
					}
				}
			} else if (packet instanceof PacketPlayOutRelEntityMoveLook) {
				int entId = refPacketRelEntityMove.getInt(packet, "a");
				if (dragons.contains(entId)) {
					PacketContainer clone = packetContainer.deepClone();
					PacketPlayOutRelEntityMoveLook newpacket = (PacketPlayOutRelEntityMoveLook)clone.getHandle();
					int dir = refPacketRelEntityMoveLook.getByte(newpacket, "e") + 128;
					if (dir > 127) dir -= 256;
					refPacketRelEntityMoveLook.setByte(newpacket, "e", (byte)dir);
					event.setPacket(clone);
					PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(entId, 0.15, 0, 0.15);
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(packet28);
				} else if (mounts.containsKey(entId)) {
					PacketContainer clone = packetContainer.deepClone();
					PacketPlayOutRelEntityMoveLook newpacket = (PacketPlayOutRelEntityMoveLook)clone.getHandle();
					refPacketRelEntityMoveLook.setInt(newpacket, "a", mounts.get(entId));
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (packet instanceof PacketPlayOutEntityLook) {
				int entId = refPacketEntityLook.getInt(packet, "a");
				if (dragons.contains(entId)) {
					PacketContainer clone = packetContainer.deepClone();
					PacketPlayOutEntityLook newpacket = (PacketPlayOutEntityLook)clone.getHandle();
					int dir = refPacketEntityLook.getByte(newpacket, "e") + 128;
					if (dir > 127) dir -= 256;
					refPacketEntityLook.setByte(newpacket, "e", (byte)dir);
					event.setPacket(clone);
					PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(entId, 0.15, 0, 0.15);
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(packet28);
				} else if (mounts.containsKey(entId)) {
					PacketContainer clone = packetContainer.deepClone();
					PacketPlayOutEntityLook newpacket = (PacketPlayOutEntityLook)clone.getHandle();
					refPacketEntityLook.setInt(newpacket, "a", mounts.get(entId));
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (packet instanceof PacketPlayOutEntityTeleport) {
				int entId = refPacketRelEntityTeleport.getInt(packet, "a");
				if (dragons.contains(entId)) {
					PacketContainer clone = packetContainer.deepClone();
					PacketPlayOutEntityTeleport newpacket = (PacketPlayOutEntityTeleport)clone.getHandle();
					int dir = refPacketRelEntityTeleport.getByte(newpacket, "e") + 128;
					if (dir > 127) dir -= 256;
					refPacketRelEntityTeleport.setByte(newpacket, "e", (byte)dir);
					event.setPacket(clone);
					PacketPlayOutEntityVelocity packet28 = new PacketPlayOutEntityVelocity(entId, 0.15, 0, 0.15);
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(packet28);
				} else if (mounts.containsKey(entId)) {
					PacketContainer clone = packetContainer.deepClone();
					PacketPlayOutEntityTeleport newpacket = (PacketPlayOutEntityTeleport)clone.getHandle();
					refPacketRelEntityTeleport.setInt(newpacket, "a", mounts.get(entId));
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (packet instanceof PacketPlayOutEntityHeadRotation) {
				int entId = refPacketEntityHeadRot.getInt(packet, "a");
				if (dragons.contains(entId)) {
					event.setCancelled(true);
				}
			}
		}
		
	}
	
	@Override
	protected void sendDestroyEntityPackets(Player disguised) {
		sendDestroyEntityPackets(disguised, disguised.getEntityId());
	}
	
	@Override
	protected void sendDestroyEntityPackets(Player disguised, int entityId) {
		DisguiseSpell.Disguise disguise = getDisguise(disguised);
		if (disguise != null && disguise.getEntityType() == EntityType.PLAYER) {
			Entity entity = getEntity(disguised, disguise);
			if (Bukkit.getPlayer(entity.getUniqueID()) == null) {
				PacketPlayOutPlayerInfo packetinfo = new PacketPlayOutPlayerInfo();
				refPacketPlayerInfo.set(packetinfo, "a", EnumPlayerInfoAction.REMOVE_PLAYER);
				List<PlayerInfoData> list = new ArrayList<PlayerInfoData>();
				list.add(packetinfo.new PlayerInfoData(((EntityHuman)entity).getProfile(), 0, EnumGamemode.SURVIVAL, new ChatComponentText(((EntityHuman)entity).getName())));
				refPacketPlayerInfo.set(packetinfo, "b", list);
				broadcastPacketGlobal(PacketType.Play.Server.PLAYER_INFO, packetinfo);
			}
		}
		PacketPlayOutEntityDestroy packet29 = new PacketPlayOutEntityDestroy(entityId);
		final EntityTracker tracker = ((CraftWorld)disguised.getWorld()).getHandle().tracker;
		tracker.a(((CraftPlayer)disguised).getHandle(), packet29);
	}
	
	private void broadcastPacketDisguised(Player disguised, PacketType packetId, Packet packet) {
		PacketContainer con = new PacketContainer(packetId, packet);
		for (Player player : protocolManager.getEntityTrackers(disguised)) {
			if (player.isValid()) {
				try {
					protocolManager.sendServerPacket(player, con, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void broadcastPacketGlobal(PacketType packetId, Packet packet) {
		PacketContainer con = new PacketContainer(packetId, packet);
		for (Player player : Bukkit.getOnlinePlayers()) {
			try {
				protocolManager.sendServerPacket(player, con, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void sendDisguisedSpawnPacket(Player viewer, Player disguised, DisguiseSpell.Disguise disguise, Entity entity) {
		if (entity == null) entity = getEntity(disguised, disguise);
		if (entity != null) {
			List<Packet> packets = getPacketsToSend(disguised, disguise, entity);
			if (packets != null && packets.size() > 0) {
				EntityPlayer ep = ((CraftPlayer)viewer).getHandle();
				try {
					for (Packet packet : packets) {
						if (packet instanceof PacketPlayOutEntityMetadata) {
							protocolManager.sendServerPacket(viewer, new PacketContainer(PacketType.Play.Server.ENTITY_METADATA, packet), false);
						} else if (packet instanceof PacketPlayOutNamedEntitySpawn) {
							protocolManager.sendServerPacket(viewer, new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN, packet), false);
						} else if (packet instanceof PacketPlayOutPlayerInfo) {
							protocolManager.sendServerPacket(viewer, new PacketContainer(PacketType.Play.Server.PLAYER_INFO, packet), false);
						} else {
							ep.playerConnection.sendPacket(packet);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	protected void sendDisguisedSpawnPackets(final Player disguised, DisguiseSpell.Disguise disguise) {
		Entity entity = getEntity(disguised, disguise);
		if (entity != null) {
			List<Packet> packets = getPacketsToSend(disguised, disguise, entity);
			if (packets != null && packets.size() > 0) {
				final EntityTracker tracker = ((CraftWorld)disguised.getWorld()).getHandle().tracker;
				for (Packet packet : packets) {
					if (packet instanceof PacketPlayOutEntityMetadata) {
						broadcastPacketDisguised(disguised, PacketType.Play.Server.ENTITY_METADATA, packet);
					} else if (packet instanceof PacketPlayOutNamedEntitySpawn) {
						broadcastPacketDisguised(disguised, PacketType.Play.Server.NAMED_ENTITY_SPAWN, packet);
					} else if (packet instanceof PacketPlayOutPlayerInfo) {
						broadcastPacketGlobal(PacketType.Play.Server.PLAYER_INFO, packet);
					} else if (packet instanceof PacketPlayOutSpawnEntityLiving) {
						broadcastPacketDisguised(disguised, PacketType.Play.Server.SPAWN_ENTITY_LIVING, packet);
					} else {
						tracker.a(((CraftPlayer)disguised).getHandle(), packet);
					}
				}
			}
			PlayerDisguiseData data = disguise.getPlayerDisguiseData();
			if (disguise.disguiseSelf() && data != null && data.skin != null && data.sig != null) {
				data = data.clone();
				data.uuid = disguised.getUniqueId().toString();
				GameProfile profile = getGameProfile(disguised.getName(), data);
				PacketPlayOutPlayerInfo packetinfo = new PacketPlayOutPlayerInfo();
				refPacketPlayerInfo.set(packetinfo, "a", EnumPlayerInfoAction.ADD_PLAYER);
				List<PlayerInfoData> list = new ArrayList<PlayerInfoData>();
				list.add(packetinfo.new PlayerInfoData(profile, 0, EnumGamemode.SURVIVAL, new ChatComponentText(disguised.getName())));
				refPacketPlayerInfo.set(packetinfo, "b", list);
				PacketPlayOutRespawn packetrespawn = new PacketPlayOutRespawn(0, EnumDifficulty.HARD, WorldType.NORMAL, EnumGamemode.getById(disguised.getGameMode().getValue()));
				List<AttributeInstance> l = new ArrayList<AttributeInstance>();
				AttributeInstance a = ((CraftPlayer)disguised).getHandle().getAttributeInstance(GenericAttributes.maxHealth);
				if (a != null) {
					l.add(a);
				}
				PacketPlayOutUpdateAttributes packetattr = new PacketPlayOutUpdateAttributes(disguised.getEntityId(), l);
				PacketPlayOutUpdateHealth packethealth = new PacketPlayOutUpdateHealth((float)disguised.getHealth(), disguised.getFoodLevel(), disguised.getSaturation());
				try {
					protocolManager.sendServerPacket(disguised, new PacketContainer(PacketType.Play.Server.PLAYER_INFO, packetinfo), false);
					protocolManager.sendServerPacket(disguised, new PacketContainer(PacketType.Play.Server.RESPAWN, packetrespawn), false);
					if (l.size() > 0) {
						protocolManager.sendServerPacket(disguised, new PacketContainer(PacketType.Play.Server.UPDATE_ATTRIBUTES, packetattr), false);
					}
					protocolManager.sendServerPacket(disguised, new PacketContainer(PacketType.Play.Server.UPDATE_HEALTH, packethealth), false);
					disguised.updateInventory();
				} catch (Exception e) {
					e.printStackTrace();
				}
				MagicSpells.scheduleDelayedTask(new Runnable() {
					public void run() {
						//disguised.updateInventory();
					}
				}, 20);
			}
		}
	}
	
	private List<Packet> getPacketsToSend(Player disguised, DisguiseSpell.Disguise disguise, Entity entity) {
		List<Packet> packets = new ArrayList<Packet>();
		if (entity instanceof EntityHuman) {
			PacketPlayOutNamedEntitySpawn packet20 = new PacketPlayOutNamedEntitySpawn((EntityHuman)entity);
			refPacketNamedEntity.setInt(packet20, "a", disguised.getEntityId());
			PacketPlayOutPlayerInfo packetinfo = new PacketPlayOutPlayerInfo();
			refPacketPlayerInfo.set(packetinfo, "a", EnumPlayerInfoAction.ADD_PLAYER);
			List<PlayerInfoData> list = new ArrayList<PlayerInfoData>();
			list.add(packetinfo.new PlayerInfoData(((EntityHuman)entity).getProfile(), 0, EnumGamemode.SURVIVAL, new ChatComponentText(((EntityHuman)entity).getName())));
			refPacketPlayerInfo.set(packetinfo, "b", list);
			packets.add(packetinfo);
			packets.add(packet20);
			PacketPlayOutEntityMetadata packet40 = new PacketPlayOutEntityMetadata(disguised.getEntityId(), entity.getDataWatcher(), false);
			packets.add(packet40);
			addEquipmentPackets(disguised, packets);
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
				addEquipmentPackets(disguised, packets);
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
			refPacketAttachEntity.setInt(packet39, "a", disguised.getEntityId());
			refPacketAttachEntity.setInt(packet39, "b", boatEntId);
			packets.add(packet39);
		}
		
		// handle passengers and vehicles
		if (disguised.getPassenger() != null) {
			PacketPlayOutAttachEntity packet39 = new PacketPlayOutAttachEntity();
			refPacketAttachEntity.setInt(packet39, "a", disguised.getPassenger().getEntityId());
			refPacketAttachEntity.setInt(packet39, "b", disguised.getEntityId());
			packets.add(packet39);
		}
		if (disguised.getVehicle() != null) {
			PacketPlayOutAttachEntity packet39 = new PacketPlayOutAttachEntity();
			refPacketAttachEntity.setInt(packet39, "a", disguised.getEntityId());
			refPacketAttachEntity.setInt(packet39, "b", disguised.getVehicle().getEntityId());
			packets.add(packet39);
		}
		
		return packets;
	}
	
	private void addEquipmentPackets(Player disguised, List<Packet> packets) {
		ItemStack inHand = disguised.getInventory().getItemInMainHand();
		if (inHand != null && inHand.getType() != Material.AIR) {
			PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(inHand));
			packets.add(packet5);
		}
		
		ItemStack offHand = disguised.getInventory().getItemInOffHand();
		if (inHand != null && inHand.getType() != Material.AIR) {
			PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(offHand));
			packets.add(packet5);
		}
		
		ItemStack helmet = disguised.getInventory().getHelmet();
		if (helmet != null && helmet.getType() != Material.AIR) {
			PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(helmet));
			packets.add(packet5);
		}
		
		ItemStack chestplate = disguised.getInventory().getChestplate();
		if (chestplate != null && chestplate.getType() != Material.AIR) {
			PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(chestplate));
			packets.add(packet5);
		}
		
		ItemStack leggings = disguised.getInventory().getLeggings();
		if (leggings != null && leggings.getType() != Material.AIR) {
			PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(leggings));
			packets.add(packet5);
		}
		
		ItemStack boots = disguised.getInventory().getBoots();
		if (boots != null && boots.getType() != Material.AIR) {
			PacketPlayOutEntityEquipment packet5 = new PacketPlayOutEntityEquipment(disguised.getEntityId(), EnumItemSlot.FEET, CraftItemStack.asNMSCopy(boots));
			packets.add(packet5);
		}
	}
	
	@Override
	protected void sendPlayerSpawnPackets(Player player) {
		PacketPlayOutNamedEntitySpawn packet20 = new PacketPlayOutNamedEntitySpawn(((CraftPlayer)player).getHandle());
		final EntityTracker tracker = ((CraftWorld)player.getWorld()).getHandle().tracker;
		tracker.a(((CraftPlayer)player).getHandle(), packet20);
	}
}