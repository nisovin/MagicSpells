package com.nisovin.magicspells.volatilecode;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_6_R3.*;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_6_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell;
import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;

public class DisguiseManager_1_6_R3 extends DisguiseManager {

	protected ProtocolManager protocolManager;
	protected PacketAdapter packetListener = null;
	
	public DisguiseManager_1_6_R3(MagicConfig config) {
		super(config);
		protocolManager = ProtocolLibrary.getProtocolManager();
		packetListener = new PacketListener();
		protocolManager.addPacketListener(packetListener);
	}
	
	@Override
	protected void cleanup() {
		protocolManager.removePacketListener(packetListener);
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
			entity = new EntityHuman(world, disguise.getNameplateText()) {
				@Override
				public void sendMessage(ChatMessage arg0) {
				}
				@Override
				public ChunkCoordinates b() {
					return null;
				}				
				@Override
				public boolean a(int arg0, String arg1) {
					return false;
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
			entity = new EntityFallingBlock(world, 0, 0, 0, id > 0 ? id : 1, data > 15 ? 0 : data);
			
		} else if (entityType == EntityType.DROPPED_ITEM) {
			int id = disguise.getVar1();
			int data = disguise.getVar2();
			entity = new EntityItem(world);
			((EntityItem)entity).setItemStack(new net.minecraft.server.v1_6_R3.ItemStack(id > 0 ? id : 1, 1, data));
			
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
			if (entityType == EntityType.IRON_GOLEM) {
				((CraftWorld)p.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity)p).getHandle(), (byte) 4);
			} else if (entityType == EntityType.WITCH) {
				((CraftWorld)p.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity)p).getHandle(), (byte) 15);
			} else if (entityType == EntityType.VILLAGER) {
				((CraftWorld)p.getWorld()).getHandle().broadcastEntityEffect(((CraftEntity)p).getHandle(), (byte) 13);
			} else if (entityType == EntityType.BLAZE || entityType == EntityType.SPIDER || entityType == EntityType.GHAST) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)0));
						broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.WITCH) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(21, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(21, Byte.valueOf((byte)0));
						broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.CREEPER && !disguise.getFlag()) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(17, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(17, Byte.valueOf((byte)0));
						broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.WOLF) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)(p.isSneaking() ? 3 : 2)));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)(p.isSneaking() ? 1 : 0)));
						broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
					}
				}, 10);
			} else if (entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)(p.isSneaking() ? 2 : 3)));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						dw.watch(16, Byte.valueOf((byte)(p.isSneaking() ? 1 : 2)));
						broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
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
		Player p = event.getPlayer();
		int entityId = p.getEntityId();
		if (entityType == EntityType.WOLF) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)0));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.ENDERMAN) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(18, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(18, Byte.valueOf((byte)0));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE) {
			if (event.isSneaking()) {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)1));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
			} else {
				final DataWatcher dw = new DataWatcher();
				dw.a(0, Byte.valueOf((byte) 0));
				dw.a(1, Short.valueOf((short) 300));
				dw.a(16, Byte.valueOf((byte)2));
				broadcastPacket(p, 40, new Packet40EntityMetadata(entityId, dw, true));
			}
		} else if (entityType == EntityType.SHEEP && event.isSneaking()) {
			p.playEffect(EntityEffect.SHEEP_EAT);
		}
	}
		
	class PacketListener extends PacketAdapter {
		
		public PacketListener() {
			super(MagicSpells.plugin, ConnectionSide.SERVER_SIDE, ListenerPriority.NORMAL, 0x14, 0x28, 0x5, 0x1F, 0x20, 0x21, 0x22, 0x23);
		}
		
		@Override
		public void onPacketSending(PacketEvent event) {
			if (event.getPacketID() == 0x14) {
				final Player player = event.getPlayer();
				final String name = event.getPacket().getStrings().getValues().get(0);
				final DisguiseSpell.Disguise disguise = disguises.get(name.toLowerCase());
				if (player != null && disguise != null) {
					event.setCancelled(true);
					Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
						public void run() {
							Player disguised = Bukkit.getPlayer(name);
							if (disguised != null) {
								sendDisguisedSpawnPacket(player, disguised, disguise, null);
							}
						}
					}, 0);
				}
			} else if (hideArmor && event.getPacketID() == 0x5) {
				Packet5EntityEquipment packet = (Packet5EntityEquipment)event.getPacket().getHandle();
				if (packet.b > 0 && disguisedEntityIds.containsKey(packet.a)) {
					event.setCancelled(true);
				}
			} else if (event.getPacketID() == 0x1F) {
				Packet31RelEntityMove packet = (Packet31RelEntityMove)event.getPacket().getHandle();
				if (mounts.containsKey(packet.a)) {
					Packet31RelEntityMove newpacket = new Packet31RelEntityMove();
					newpacket.a = mounts.get(packet.a);
					newpacket.b = packet.b;
					newpacket.c = packet.c;
					newpacket.d = packet.d;
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (event.getPacketID() == 0x20) {
				Packet32EntityLook packet = (Packet32EntityLook)event.getPacket().getHandle();
				if (dragons.contains(packet.a)) {
					Packet32EntityLook newpacket = new Packet32EntityLook();
					newpacket.a = -packet.a;
					int dir = packet.e + 128;
					if (dir > 127) dir -= 256;
					newpacket.e = (byte)dir;
					newpacket.f = 0;
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
					event.setCancelled(true);
				} else if (mounts.containsKey(packet.a)) {
					Packet32EntityLook newpacket = new Packet32EntityLook();
					newpacket.a = mounts.get(packet.a);
					newpacket.e = packet.e;
					newpacket.f = packet.f;
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (event.getPacketID() == 0x21) {
				Packet33RelEntityMoveLook packet = (Packet33RelEntityMoveLook)event.getPacket().getHandle();
				if (dragons.contains(packet.a)) {
					Packet33RelEntityMoveLook newpacket = new Packet33RelEntityMoveLook();
					newpacket.a = -packet.a;
					newpacket.b = packet.b;
					newpacket.c = packet.c;
					newpacket.d = packet.d;
					int dir = packet.e + 128;
					if (dir > 127) dir -= 256;
					newpacket.e = (byte)dir;
					newpacket.f = 0;
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
					Packet28EntityVelocity packet28 = new Packet28EntityVelocity(packet.a, 0.15, 0, 0.15);
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(packet28);
					event.setCancelled(true);
				} else if (mounts.containsKey(packet.a)) {
					Packet33RelEntityMoveLook newpacket = new Packet33RelEntityMoveLook();
					newpacket.a = mounts.get(packet.a);
					newpacket.b = packet.b;
					newpacket.c = packet.c;
					newpacket.d = packet.d;
					newpacket.e = packet.e;
					newpacket.f = packet.f;
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (event.getPacketID() == 0x22) {
				Packet34EntityTeleport packet = (Packet34EntityTeleport)event.getPacket().getHandle();
				if (dragons.contains(packet.a)) {
					Packet34EntityTeleport newpacket = new Packet34EntityTeleport();
					newpacket.a = -packet.a;
					newpacket.b = packet.b;
					newpacket.c = packet.c;
					newpacket.d = packet.d;
					int dir = packet.e + 128;
					if (dir > 127) dir -= 256;
					newpacket.e = (byte)dir;
					newpacket.f = 0;
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
					event.setCancelled(true);
				} else if (mounts.containsKey(packet.a)) {
					Packet34EntityTeleport newpacket = new Packet34EntityTeleport();
					newpacket.a = mounts.get(packet.a);
					newpacket.b = packet.b;
					newpacket.c = packet.c;
					newpacket.d = packet.d;
					newpacket.e = packet.e;
					newpacket.f = packet.f;
					((CraftPlayer)event.getPlayer()).getHandle().playerConnection.sendPacket(newpacket);
				}
			} else if (event.getPacketID() == 0x23) {
				Packet35EntityHeadRotation packet = (Packet35EntityHeadRotation)event.getPacket().getHandle();
				if (dragons.contains(packet.a)) {
					event.setCancelled(true);
				}
			} else if (event.getPacketID() == 0x28) {
				Packet40EntityMetadata packet = (Packet40EntityMetadata)event.getPacket().getHandle();
				DisguiseSpell.Disguise disguise = disguisedEntityIds.get(packet.a);
				if (disguise != null && disguise.getEntityType() != EntityType.PLAYER) {
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
		Packet29DestroyEntity packet29 = new Packet29DestroyEntity(entityId);
		final EntityTracker tracker = ((CraftWorld)disguised.getWorld()).getHandle().tracker;
		tracker.a(((CraftPlayer)disguised).getHandle(), packet29);
	}
	
	private void broadcastPacket(Player disguised, int packetId, Packet packet) {
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
	
	private void sendDisguisedSpawnPacket(Player viewer, Player disguised, DisguiseSpell.Disguise disguise, Entity entity) {
		if (entity == null) entity = getEntity(disguised, disguise);
		if (entity != null) {
			List<Packet> packets = getPacketsToSend(disguised, disguise, entity);
			if (packets != null && packets.size() > 0) {
				EntityPlayer ep = ((CraftPlayer)viewer).getHandle();
				try {
					for (Packet packet : packets) {
						if (packet instanceof Packet40EntityMetadata) {
							protocolManager.sendServerPacket(viewer, new PacketContainer(40, packet), false);
						} else if (packet instanceof Packet20NamedEntitySpawn) {
							protocolManager.sendServerPacket(viewer, new PacketContainer(20, packet), false);
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
	protected void sendDisguisedSpawnPackets(Player disguised, DisguiseSpell.Disguise disguise) {
		Entity entity = getEntity(disguised, disguise);
		if (entity != null) {
			List<Packet> packets = getPacketsToSend(disguised, disguise, entity);
			if (packets != null && packets.size() > 0) {
				final EntityTracker tracker = ((CraftWorld)disguised.getWorld()).getHandle().tracker;
				for (Packet packet : packets) {
					if (packet instanceof Packet40EntityMetadata) {
						broadcastPacket(disguised, 40, packet);
					} else if (packet instanceof Packet20NamedEntitySpawn) {
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
			Packet20NamedEntitySpawn packet20 = new Packet20NamedEntitySpawn((EntityHuman)entity);
			packet20.a = disguised.getEntityId();
			packets.add(packet20);
			
			ItemStack inHand = disguised.getItemInHand();
			if (inHand != null && inHand.getType() != Material.AIR) {
				Packet5EntityEquipment packet5 = new Packet5EntityEquipment(disguised.getEntityId(), 0, CraftItemStack.asNMSCopy(inHand));
				packets.add(packet5);
			}
			
			ItemStack helmet = disguised.getInventory().getHelmet();
			if (helmet != null && helmet.getType() != Material.AIR) {
				Packet5EntityEquipment packet5 = new Packet5EntityEquipment(disguised.getEntityId(), 4, CraftItemStack.asNMSCopy(helmet));
				packets.add(packet5);
			}
			
			ItemStack chestplate = disguised.getInventory().getChestplate();
			if (chestplate != null && chestplate.getType() != Material.AIR) {
				Packet5EntityEquipment packet5 = new Packet5EntityEquipment(disguised.getEntityId(), 3, CraftItemStack.asNMSCopy(chestplate));
				packets.add(packet5);
			}
			
			ItemStack leggings = disguised.getInventory().getLeggings();
			if (leggings != null && leggings.getType() != Material.AIR) {
				Packet5EntityEquipment packet5 = new Packet5EntityEquipment(disguised.getEntityId(), 2, CraftItemStack.asNMSCopy(leggings));
				packets.add(packet5);
			}
			
			ItemStack boots = disguised.getInventory().getBoots();
			if (boots != null && boots.getType() != Material.AIR) {
				Packet5EntityEquipment packet5 = new Packet5EntityEquipment(disguised.getEntityId(), 1, CraftItemStack.asNMSCopy(boots));
				packets.add(packet5);
			}
		} else if (entity instanceof EntityLiving) {
			Packet24MobSpawn packet24 = new Packet24MobSpawn((EntityLiving)entity);
			packet24.a = disguised.getEntityId();
			if (dragons.contains(disguised.getEntityId())) {
				int dir = packet24.i + 128;
				if (dir > 127) dir -= 256;
				packet24.i = (byte)dir;
				packet24.j = 0;
				packet24.k = 1;
			}
			packets.add(packet24);
			Packet40EntityMetadata packet40 = new Packet40EntityMetadata(disguised.getEntityId(), entity.getDataWatcher(), false);
			packets.add(packet40);
			if (dragons.contains(disguised.getEntityId())) {
				Packet28EntityVelocity packet28 = new Packet28EntityVelocity(disguised.getEntityId(), 0.15, 0, 0.15);
				packets.add(packet28);
			}
			
			if (disguise.getEntityType() == EntityType.ZOMBIE || disguise.getEntityType() == EntityType.SKELETON) {
				ItemStack inHand = disguised.getItemInHand();
				if (inHand != null && inHand.getType() != Material.AIR) {
					Packet5EntityEquipment packet5 = new Packet5EntityEquipment(disguised.getEntityId(), 0, CraftItemStack.asNMSCopy(inHand));
					packets.add(packet5);
				}
			}
		} else if (entity instanceof EntityFallingBlock) {
			Packet23VehicleSpawn packet23 = new Packet23VehicleSpawn(entity, 70, disguise.getVar1() | ((byte)disguise.getVar2()) << 16);
			packet23.a = disguised.getEntityId();
			packets.add(packet23);
		} else if (entity instanceof EntityItem) {
			Packet23VehicleSpawn packet23 = new Packet23VehicleSpawn(entity, 2, 1);
			packet23.a = disguised.getEntityId();
			packets.add(packet23);
			Packet40EntityMetadata packet40 = new Packet40EntityMetadata(disguised.getEntityId(), entity.getDataWatcher(), true);
			packets.add(packet40);
		}
		
		if (disguise.isRidingBoat()) {
			EntityBoat boat = new EntityBoat(entity.world);
			if (mounts.containsKey(disguised.getEntityId())) {
				boat.id = mounts.get(disguised.getEntityId());
			} else {
				mounts.put(disguised.getEntityId(), boat.id);
			}
			boat.setPositionRotation(disguised.getLocation().getX(), disguised.getLocation().getY(), disguised.getLocation().getZ(), disguised.getLocation().getYaw(), 0);
			Packet23VehicleSpawn packet23 = new Packet23VehicleSpawn(boat, 1);
			packets.add(packet23);
			Packet39AttachEntity packet39 = new Packet39AttachEntity();
			packet39.a = disguised.getEntityId();
			packet39.b = boat.id;
			packets.add(packet39);
		}
		
		// handle passengers and vehicles
		if (disguised.getPassenger() != null) {
			Packet39AttachEntity packet39 = new Packet39AttachEntity();
			packet39.a = disguised.getPassenger().getEntityId();
			packet39.b = disguised.getEntityId();
			packets.add(packet39);
		}
		if (disguised.getVehicle() != null) {
			Packet39AttachEntity packet39 = new Packet39AttachEntity();
			packet39.a = disguised.getEntityId();
			packet39.b = disguised.getVehicle().getEntityId();
			packets.add(packet39);
		}
		
		return packets;
	}
	
	@Override
	protected void sendPlayerSpawnPackets(Player player) {
		Packet20NamedEntitySpawn packet20 = new Packet20NamedEntitySpawn(((CraftPlayer)player).getHandle());
		final EntityTracker tracker = ((CraftWorld)player.getWorld()).getHandle().tracker;
		tracker.a(((CraftPlayer)player).getHandle(), packet20);
	}
}