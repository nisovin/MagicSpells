package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ProjectileSpell extends InstantSpell {

	private Class<? extends Projectile> projectileClass;
	private ItemStack projectileItem;
	private double velocity;
	private boolean requireHitEntity;
	private boolean cancelDamage;
	private boolean removeProjectile;
	private int maxDistanceSquared;
	private List<String> spellNames;
	private List<TargetedSpell> spells;
	private int aoeRadius;
	private boolean targetPlayers;
	private boolean allowTargetChange;
	private String strHitCaster;
	private String strHitTarget;
	
	private HashMap<Projectile, ProjectileInfo> projectiles;
	private HashMap<Item, ProjectileInfo> itemProjectiles;
	
	public ProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String projectileType = getConfigString("projectile", "arrow");
		if (projectileType.equalsIgnoreCase("arrow")) {
			projectileClass = Arrow.class;
		} else if (projectileType.equalsIgnoreCase("snowball")) {
			projectileClass = Snowball.class;
		} else if (projectileType.equalsIgnoreCase("egg")) {
			projectileClass = Egg.class;
		} else if (projectileType.equalsIgnoreCase("enderpearl")) {
			projectileClass = EnderPearl.class;
		} else if (projectileType.equalsIgnoreCase("potion")) {
			projectileClass = ThrownPotion.class;
		} else if (projectileType.matches("[0-9]+(:[0-9]+)?")) {
			String[] s = projectileType.split(":");
			int type = Integer.parseInt(s[0]);
			short data = 0;
			if (s.length > 1) {
				data = Short.parseShort(s[1]);
			}
			projectileItem = new ItemStack(type, 0, data);
		}
		if (projectileClass == null && projectileItem == null) {
			MagicSpells.error("Invalid projectile type on spell '" + internalName + "'");
		}
		velocity = getConfigFloat("velocity", 0);
		requireHitEntity = getConfigBoolean("require-hit-entity", false);
		cancelDamage = getConfigBoolean("cancel-damage", true);
		removeProjectile = getConfigBoolean("remove-projectile", true);
		maxDistanceSquared = getConfigInt("max-distance", 0);
		maxDistanceSquared = maxDistanceSquared * maxDistanceSquared;
		spellNames = getConfigStringList("spells", null);
		aoeRadius = getConfigInt("aoe-radius", 0);
		targetPlayers = getConfigBoolean("target-players", false);
		allowTargetChange = getConfigBoolean("allow-target-change", true);
		strHitCaster = getConfigString("str-hit-caster", "");
		strHitTarget = getConfigString("str-hit-target", "");
		
		if (projectileClass != null) {
			projectiles = new HashMap<Projectile, ProjectileInfo>();
		} else if (projectileItem != null) {
			itemProjectiles = new HashMap<Item, ProjectileSpell.ProjectileInfo>();
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		spells = new ArrayList<TargetedSpell>();
		if (spellNames != null) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null && (spell instanceof TargetedEntitySpell || spell instanceof TargetedLocationSpell)) {
					spells.add((TargetedSpell)spell);
				} else {
					MagicSpells.error("Projectile spell '" + internalName + "' attempted to add invalid spell '" + spellName + "'.");
				}
			}
		}
		if (spells.size() == 0) {
			MagicSpells.error("Projectile spell '" + internalName + "' has no spells!");
		}
		
		if (projectileClass != null) {
			if (projectileClass == EnderPearl.class) {
				registerEvents(new EnderTpListener());
			} else if (projectileClass == Egg.class) {
				registerEvents(new EggListener());
			} else if (projectileClass == ThrownPotion.class) {
				registerEvents(new PotionListener());
			}
		} else if (projectileItem != null) {
			registerEvents(new PickupListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (projectileClass != null) {
				Projectile projectile = player.launchProjectile(projectileClass);
				if (velocity > 0) {
					projectile.setVelocity(player.getLocation().getDirection().multiply(velocity));
				}
				projectiles.put(projectile, new ProjectileInfo(player, power));
				playGraphicalEffects(1, projectile);
			} else if (projectileItem != null) {
				Item item = player.getWorld().dropItem(player.getEyeLocation(), projectileItem.clone());
				item.setVelocity(player.getLocation().getDirection().multiply(velocity > 0 ? velocity : 1));
				item.setPickupDelay(10);
				itemProjectiles.put(item, new ProjectileInfo(player, power, new ItemProjectileMonitor(item)));
				playGraphicalEffects(1, item);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public boolean projectileHitEntity(Entity projectile, LivingEntity target, ProjectileInfo info) {
		if (!info.done && (maxDistanceSquared == 0 || projectile.getLocation().distanceSquared(info.start) <= maxDistanceSquared)) { 
			
			if (aoeRadius == 0) {
				// check player
				if (!targetPlayers && target instanceof Player) return false;
				
				// call target event
				SpellTargetEvent evt = new SpellTargetEvent(this, info.player, target);
				Bukkit.getPluginManager().callEvent(evt);
				if (evt.isCancelled()) {
					return false;
				} else if (allowTargetChange) {
					target = evt.getTarget();
				}
				
				// run spells
				for (TargetedSpell spell : spells) {
					if (spell instanceof TargetedEntitySpell) {
						((TargetedEntitySpell)spell).castAtEntity(info.player, target, info.power);
						playGraphicalEffects(2, target);
					} else if (spell instanceof TargetedLocationSpell) {
						((TargetedLocationSpell)spell).castAtLocation(info.player, target.getLocation(), info.power);
						playGraphicalEffects(2, target.getLocation());
					}
				}
				
				// send messages
				String entityName;
				if (target instanceof Player) {
					entityName = ((Player)target).getDisplayName();
				} else {
					EntityType entityType = target.getType();
					entityName = MagicSpells.getEntityNames().get(entityType);
					if (entityName == null) {
						entityName = entityType.getName();
					}
				}
				sendMessage(info.player, strHitCaster, "%t", entityName);
				if (target instanceof Player) {
					sendMessage((Player)target, strHitTarget, "%a", info.player.getDisplayName());
				}
			} else {
				aoe(projectile, info);
			}
			
			info.done = true;
			
		}
		return true;
	}
	
	private boolean projectileHitLocation(Entity projectile, ProjectileInfo info) {
		if (!requireHitEntity && !info.done && (maxDistanceSquared == 0 || projectile.getLocation().distanceSquared(info.start) <= maxDistanceSquared)) {
			if (aoeRadius == 0) {
				for (TargetedSpell spell : spells) {
					if (spell instanceof TargetedLocationSpell) {
						((TargetedLocationSpell)spell).castAtLocation(info.player, projectile.getLocation(), info.power);
						playGraphicalEffects(2, projectile.getLocation());
					}
				}
				sendMessage(info.player, strHitCaster);
			} else {
				aoe(projectile, info);
			}
			info.done = true;
		}
		return true;
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Projectile)) return;
		
		Projectile projectile = (Projectile)event.getDamager();
		ProjectileInfo info = projectiles.get(projectile);
		if (info == null || event.isCancelled()) return;
		
		if (!(event.getEntity() instanceof LivingEntity)) {
			return;
		}
		
		boolean ok = projectileHitEntity(projectile, (LivingEntity)event.getEntity(), info);		
		
		if (ok && cancelDamage) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		final Projectile projectile = (Projectile)event.getEntity();
		ProjectileInfo info = projectiles.get(projectile);
		if (info != null) {
			projectileHitLocation(projectile, info);
			
			// remove it from world
			if (removeProjectile) {
				projectile.remove();
			}
			// remove it at end of tick
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					projectiles.remove(projectile);
				}
			}, 0);
		}
	}
	
	private void aoe(Entity projectile, ProjectileInfo info) {
		playGraphicalEffects(4, projectile.getLocation());
		List<Entity> entities = projectile.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius);
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity && (targetPlayers || !(entity instanceof Player)) && !entity.equals(info.player)) {
				LivingEntity target = (LivingEntity)entity;
				
				// call target event
				SpellTargetEvent evt = new SpellTargetEvent(this, info.player, target);
				Bukkit.getPluginManager().callEvent(evt);
				if (evt.isCancelled()) {
					continue;
				} else if (allowTargetChange) {
					target = evt.getTarget();
				}
				
				// run spells
				for (TargetedSpell spell : spells) {
					if (spell instanceof TargetedEntitySpell) {
						((TargetedEntitySpell)spell).castAtEntity(info.player, target, info.power);
						playGraphicalEffects(2, target);
					} else if (spell instanceof TargetedLocationSpell) {
						((TargetedLocationSpell)spell).castAtLocation(info.player, target.getLocation(), info.power);
						playGraphicalEffects(2, target.getLocation());
					}
				}
				
				// send message if player
				if (target instanceof Player) {
					sendMessage((Player)target, strHitTarget, "%a", info.player.getDisplayName());
				}
			}
		}
		sendMessage(info.player, strHitCaster);
	}
	
	public class EnderTpListener implements Listener {
		@EventHandler(ignoreCancelled=true)
		public void onPlayerTeleport(PlayerTeleportEvent event) {
			if (event.getCause() == TeleportCause.ENDER_PEARL) {
				for (Projectile projectile : projectiles.keySet()) {
					if (locationsEqual(projectile.getLocation(), event.getTo())) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}
	
	public class EggListener implements Listener {
		@EventHandler(ignoreCancelled=true)
		public void onCreatureSpawn(CreatureSpawnEvent event) {
			if (event.getSpawnReason() == SpawnReason.EGG) {
				for (Projectile projectile : projectiles.keySet()) {
					if (locationsEqual(projectile.getLocation(), event.getLocation())) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}
	
	public class PotionListener implements Listener {
		@EventHandler(ignoreCancelled=true)
		public void onPotionSplash(PotionSplashEvent event) {
			if (projectiles.containsKey(event.getPotion())) {
				event.setCancelled(true);
			}
		}
	}
	
	public class PickupListener implements Listener {
		@EventHandler(ignoreCancelled=true)
		public void onPickupItem(PlayerPickupItemEvent event) {
			Item item = event.getItem();
			ProjectileInfo info = itemProjectiles.get(item);
			if (info != null) {
				event.setCancelled(true);
				projectileHitEntity(item, event.getPlayer(), info);
				item.remove();
				itemProjectiles.remove(item);
				info.monitor.stop();
			}
		}
	}
	
	private boolean locationsEqual(Location loc1, Location loc2) {
		return 
				Math.abs(loc1.getX() - loc2.getX()) < 0.1
				&& Math.abs(loc1.getY() - loc2.getY()) < 0.1
				&& Math.abs(loc1.getZ() - loc2.getZ()) < 0.1;
	}
	
	private class ProjectileInfo {
		Player player;
		Location start;
		float power;
		boolean done;
		ItemProjectileMonitor monitor;
		
		public ProjectileInfo(Player player, float power) {
			this.player = player;
			this.start = player.getLocation().clone();
			this.power = power;
			this.done = false;
			this.monitor = null;
		}
		
		public ProjectileInfo(Player player, float power, ItemProjectileMonitor monitor) {
			this(player, power);
			this.monitor = monitor;
		}
	}
	
	private class ItemProjectileMonitor implements Runnable {

		Item item;
		int taskId;
		int count;
		
		public ItemProjectileMonitor(Item item) {
			this.item = item;
			this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 1, 1);
			this.count = 0;
		}
		
		@Override
		public void run() {
			Vector v = item.getVelocity();
			if (Math.abs(v.getY()) < .01 || (Math.abs(v.getX()) < .01 && Math.abs(v.getZ()) < .01)) {
				ProjectileInfo info = itemProjectiles.get(item);
				if (info != null) {
					projectileHitLocation(item, info);
					stop();
				}
			}
			if (++count > 100) {
				stop();
			}
		}
		
		public void stop() {
			item.remove();
			itemProjectiles.remove(item);
			Bukkit.getScheduler().cancelTask(taskId);
		}
		
	}

}
