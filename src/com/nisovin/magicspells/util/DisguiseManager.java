package com.nisovin.magicspells.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell;

public abstract class DisguiseManager implements Listener {

	protected boolean hideArmor;
	
	protected Set<DisguiseSpell> disguiseSpells = new HashSet<DisguiseSpell>();
	protected Map<String, DisguiseSpell.Disguise> disguises = new ConcurrentHashMap<String, DisguiseSpell.Disguise>();
	protected Map<Integer, DisguiseSpell.Disguise> disguisedEntityIds = new ConcurrentHashMap<Integer, DisguiseSpell.Disguise>();
	protected Set<Integer> dragons = Collections.synchronizedSet(new HashSet<Integer>());
	protected Map<Integer, Integer> mounts = new ConcurrentHashMap<Integer, Integer>();

	protected Random random = new Random();

	public DisguiseManager(MagicConfig config) {
		this.hideArmor = config.getBoolean("general.disguise-spell-hide-armor", false);
		Bukkit.getPluginManager().registerEvents(this, MagicSpells.plugin);
	}
	
	public void registerSpell(DisguiseSpell spell) {
		disguiseSpells.add(spell);
	}
	
	public void unregisterSpell(DisguiseSpell spell) {
		disguiseSpells.remove(spell);
	}
	
	public int registeredSpellsCount() {
		return disguiseSpells.size();
	}
	
	public void addDisguise(Player player, DisguiseSpell.Disguise disguise) {
		if (isDisguised(player)) {
			removeDisguise(player);
		}
		disguises.put(player.getName().toLowerCase(), disguise);
		disguisedEntityIds.put(player.getEntityId(), disguise);
		if (disguise.getEntityType() == EntityType.ENDER_DRAGON) {
			dragons.add(player.getEntityId());
		}
		applyDisguise(player, disguise);
	}
	
	public void removeDisguise(Player player) {
		removeDisguise(player, true);
	}
	
	public void removeDisguise(Player player, boolean sendPlayerPackets) {
		removeDisguise(player, sendPlayerPackets, true);
	}
	
	public void removeDisguise(Player player, boolean sendPlayerPackets, boolean delaySpawnPacket) {
		DisguiseSpell.Disguise disguise = disguises.get(player.getName().toLowerCase());
		disguisedEntityIds.remove(player.getEntityId());
		dragons.remove(player.getEntityId());
		if (disguise != null) {
			clearDisguise(player, sendPlayerPackets, delaySpawnPacket);
			disguise.getSpell().undisguise(player);
			disguises.remove(player.getName().toLowerCase());
		}
		mounts.remove(player.getEntityId());
	}
	
	public boolean isDisguised(Player player) {
		return disguises.containsKey(player.getName().toLowerCase());
	}
	
	public DisguiseSpell.Disguise getDisguise(Player player) {
		return disguises.get(player.getName().toLowerCase());
	}
	
	public void destroy() {
		HandlerList.unregisterAll(this);
		cleanup();
		
		disguises.clear();
		disguisedEntityIds.clear();
		dragons.clear();
		mounts.clear();
		disguiseSpells.clear();
	}
	
	protected abstract void cleanup();
	
	private void applyDisguise(final Player player, final DisguiseSpell.Disguise disguise) {
		sendDestroyEntityPackets(player);
		MagicSpells.scheduleDelayedTask(new Runnable() {
			public void run() {
				sendDisguisedSpawnPackets(player, disguise);
			}
		}, 5);
	}
	
	private void clearDisguise(final Player player, boolean sendPlayerPackets, boolean delaySpawnPacket) {
		if (sendPlayerPackets) {
			sendDestroyEntityPackets(player);
		}
		if (mounts.containsKey(player.getEntityId())) {
			sendDestroyEntityPackets(player, mounts.remove(player.getEntityId()));
		}
		if (sendPlayerPackets && player.isValid()) {
			if (delaySpawnPacket) {
				MagicSpells.scheduleDelayedTask(new Runnable() {
					public void run() {
						sendPlayerSpawnPackets(player);
					}
				}, 5);
			} else {
				sendPlayerSpawnPackets(player);
			}
		}
	}
	
	protected abstract void sendDestroyEntityPackets(Player disguised);
	
	protected abstract void sendDestroyEntityPackets(Player disguised, int entityId);
	
	protected abstract void sendDisguisedSpawnPackets(Player disguised, DisguiseSpell.Disguise disguise);
	
	protected abstract void sendPlayerSpawnPackets(Player player);
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		disguisedEntityIds.remove(event.getPlayer().getEntityId());
		dragons.remove(event.getPlayer().getEntityId());
		if (mounts.containsKey(event.getPlayer().getEntityId())) {
			sendDestroyEntityPackets(event.getPlayer(), mounts.remove(event.getPlayer().getEntityId()));
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		if (isDisguised(p)) {
			disguisedEntityIds.put(p.getEntityId(), getDisguise(p));
			if (getDisguise(p).getEntityType() == EntityType.ENDER_DRAGON) {
				dragons.add(p.getEntityId());
			}
		}
	}
	
}
