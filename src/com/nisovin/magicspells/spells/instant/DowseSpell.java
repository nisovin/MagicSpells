package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class DowseSpell extends InstantSpell {

	private MagicMaterial material;
	private EntityType entityType;
	private String playerName;
	private int radius;
	private boolean rotatePlayer;
	private boolean setCompass;
	private String strNotFound;
	
	private boolean getDistance;
	
	public DowseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String blockName = getConfigString("block-type", "");
		if (!blockName.isEmpty()) {
			material = MagicSpells.getItemNameResolver().resolveBlock(blockName);
		}
		String entityName = getConfigString("entity-type", "");
		if (!entityName.isEmpty()) {
			if (entityName.equalsIgnoreCase("player")) {
				entityType = EntityType.PLAYER;
			} else if (entityName.toLowerCase().startsWith("player:")) {
				entityType = EntityType.PLAYER;
				playerName = entityName.split(":")[1];
			} else {
				entityType = Util.getEntityType(entityName);
			}
		}
		
		radius = getConfigInt("radius", 4);
		rotatePlayer = getConfigBoolean("rotate-player", true);
		setCompass = getConfigBoolean("set-compass", true);
		strNotFound = getConfigString("str-not-found", "No dowsing target found.");
		
		getDistance = strCastSelf != null && strCastSelf.contains("%d");
		
		if (material == null && entityType == null) {
			MagicSpells.error("DowseSpell '" + internalName + "' has no dowse target (block or entity) defined");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			
			int distance = -1;
			
			if (material != null) {
			
				Block foundBlock = null;
				
				Location loc = player.getLocation();
				World world = player.getWorld();
				int cx = loc.getBlockX();
				int cy = loc.getBlockY();
				int cz = loc.getBlockZ();
				for (int r = 1; r <= Math.round(radius * power); r++) {
					for (int x = -r; x <= r; x++) {
						for (int y = -r; y <= r; y++) {
							for (int z = -r; z <= r; z++) {
								if (x == r || y == r || z == r || -x == r || -y == r || -z == r) {
									Block block = world.getBlockAt(cx + x, cy + y, cz + z);
									if (material.equals(block)) {
										foundBlock = block;
										break;
									}
								}
							}
							if (foundBlock != null) break;
						}
						if (foundBlock != null) break;
					}
					if (foundBlock != null) break;
				}
							
				if (foundBlock == null) {
					sendMessage(player, strNotFound);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					if (rotatePlayer) {
						Vector v = foundBlock.getLocation().add(.5, .5, .5).subtract(player.getEyeLocation()).toVector().normalize();
						Util.setFacing(player, v);
					}
					if (setCompass) {
						player.setCompassTarget(foundBlock.getLocation());
					}
					if (getDistance) {
						distance = (int)Math.round(player.getLocation().distance(foundBlock.getLocation()));
					}
				}
				
			} else if (entityType != null) {

				// find entity
				Entity foundEntity = null;
				double distanceSq = radius * radius;
				if (entityType == EntityType.PLAYER && playerName != null) {
					// find specific player
					foundEntity = Bukkit.getPlayerExact(playerName);
					if (foundEntity != null) {
						if (!foundEntity.getWorld().equals(player.getWorld())) {
							foundEntity = null;
						} else if (radius > 0 && player.getLocation().distanceSquared(foundEntity.getLocation()) > distanceSq) {
							foundEntity = null;
						}
					}
				} else {
					// find nearest entity
					List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
					Location playerLoc = player.getLocation();
					TreeSet<NearbyEntity> ordered = new TreeSet<NearbyEntity>();
					for (Entity e : nearby) {
						if (e.getType() == entityType) {
							double d = e.getLocation().distanceSquared(playerLoc);
							if (d < distanceSq) {
								ordered.add(new NearbyEntity(e, d));
							}
						}
					}
					if (ordered.size() > 0) {
						for (NearbyEntity ne : ordered) {
							if (ne.entity instanceof LivingEntity) {
								SpellTargetEvent event = new SpellTargetEvent(this, player, (LivingEntity)ne.entity, power);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled()) {
									foundEntity = ne.entity;
									break;
								}
							} else {
								foundEntity = ne.entity;
								break;
							}
						}
					}
				}
				
				
				if (foundEntity == null) {
					sendMessage(player, strNotFound);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					if (rotatePlayer) {
						Location l = (foundEntity instanceof LivingEntity ? ((LivingEntity)foundEntity).getEyeLocation() : foundEntity.getLocation());
						Vector v = l.subtract(player.getEyeLocation()).toVector().normalize();
						Util.setFacing(player, v);
					}
					if (setCompass) {
						player.setCompassTarget(foundEntity.getLocation());
					}
					if (getDistance) {
						distance = (int)Math.round(player.getLocation().distance(foundEntity.getLocation()));
					}
				}
			}
			
			playSpellEffects(EffectPosition.CASTER, player);
			if (getDistance) {
				sendMessage(player, strCastSelf, "%d", distance+"");
				sendMessageNear(player, strCastOthers);
				return PostCastAction.NO_MESSAGES;
			}
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	class NearbyEntity implements Comparable<NearbyEntity> {

		Entity entity;
		double distanceSquared;
		
		public NearbyEntity(Entity entity, double distanceSquared) {
			this.entity = entity;
			this.distanceSquared = distanceSquared;
		}
		
		@Override
		public int compareTo(NearbyEntity e) {
			if (e.distanceSquared < this.distanceSquared) {
				return -1;
			} else if (e.distanceSquared > this.distanceSquared) {
				return 1;
			} else {
				return 0;
			}
		}
		
	}

}
