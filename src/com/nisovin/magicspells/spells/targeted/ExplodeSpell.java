package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.Random;

import net.minecraft.server.EntityTNTPrimed;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ExplodeSpell extends TargetedSpell {
	
	private boolean checkPlugins;
	private int explosionSize;
	private int backfireChance;
	private boolean preventBlockDamage;
	private float damageMultiplier;
	private String strNoTarget;
	
	private HashMap<Player,Float> recentlyExploded;
	
	public ExplodeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		checkPlugins = getConfigBoolean("check-plugins", true);
		explosionSize = getConfigInt("explosion-size", 4);
		backfireChance = getConfigInt("backfire-chance", 0);
		preventBlockDamage = getConfigBoolean("prevent-block-damage", false);
		damageMultiplier = getConfigFloat("damage-multiplier", 0);
		strNoTarget = getConfigString("str-no-target", "Cannot explode there.");
		
		if (preventBlockDamage || damageMultiplier > 0) {
			recentlyExploded = new HashMap<Player,Float>();
		}
	}
	
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = player.getTargetBlock(null, range);
			if (target == null || target.getType() == Material.AIR) {
				// fail: no target
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				// backfire chance
				if (backfireChance > 0) {
					Random rand = new Random();
					if (rand.nextInt(10000) < backfireChance) {
						target = player.getLocation().getBlock();
					}					
				}
				if (checkPlugins) {
					// check plugins
					EntityTNTPrimed e = new EntityTNTPrimed(((CraftWorld)target.getWorld()).getHandle(), target.getX(), target.getY(), target.getZ());
					CraftTNTPrimed c = new CraftTNTPrimed((CraftServer)Bukkit.getServer(), e);
					ExplosionPrimeEvent event = new ExplosionPrimeEvent(c, explosionSize*power, false);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						sendMessage(player, strNoTarget);
						fizzle(player);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
				if (preventBlockDamage || damageMultiplier > 0) {
					recentlyExploded.put(player, power);
				}
				createExplosion(player, target.getLocation(), explosionSize*power);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public void createExplosion(Player player, Location location, float size) {
		((CraftWorld)location.getWorld()).getHandle().createExplosion(((CraftPlayer)player).getHandle(), location.getX(), location.getY(), location.getZ(), size, false);
	}

	@EventHandler(priority=EventPriority.HIGH)
	public void onEntityDamage(EntityDamageEvent event) {
		if (damageMultiplier > 0 && !event.isCancelled() && event instanceof EntityDamageByEntityEvent && event.getCause() == DamageCause.ENTITY_EXPLOSION) {
			EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
			if (evt.getDamager() instanceof Player && recentlyExploded.containsKey(evt.getDamager())) {
				float power = recentlyExploded.get(evt.getDamager());
				event.setDamage(Math.round(event.getDamage() * damageMultiplier * power));
			}
		}
	}
	
	@EventHandler
	public void onExplode(EntityExplodeEvent event) {
		if (event.isCancelled() || !preventBlockDamage) {
			recentlyExploded.remove(event.getEntity());
			return;
		}
		
		if (event.getEntity() instanceof Player && recentlyExploded.containsKey(event.getEntity())) {
			event.blockList().clear();
			event.setYield(0);
			recentlyExploded.remove(event.getEntity());
		}
	}
	
}