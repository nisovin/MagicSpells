package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.BlockPlatform;
import com.nisovin.magicspells.util.MagicConfig;

public class CarpetSpell extends BuffSpell {
	
	private Material platformBlock;
	private int size;
	private boolean cancelOnTeleport;
	
	private HashMap<String,BlockPlatform> windwalkers;
	private HashSet<Player> falling;
	private Listener listener;

	public CarpetSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("platform-block", "glass"));
		if (m != null) {
			platformBlock = m.getMaterial();
		} else {
			platformBlock = Material.GLASS;
		}
		size = getConfigInt("size", 2);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", true);
		
		windwalkers = new HashMap<String,BlockPlatform>();
		falling = new HashSet<Player>();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (cancelOnLogout) {
			registerEvents(new QuitListener());
		}
		if (cancelOnTeleport) {
			registerEvents(new TeleportListener());
		}
	}
	
	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		windwalkers.put(player.getName(), new BlockPlatform(platformBlock, Material.AIR, player.getLocation().getBlock().getRelative(0,-1,0), size, true, "square"));
		registerListener();
		return true;
	}
	
	private void registerListener() {
		if (listener == null) {
			listener = new CarpetListener();
			registerEvents(listener);
		}
	}
	
	private void unregisterListener() {
		if (listener != null && windwalkers.size() == 0) {
			unregisterEvents(listener);
			listener = null;
		}
	}

	public class CarpetListener implements Listener {
	
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerMove(PlayerMoveEvent event) {
			BlockPlatform platform = windwalkers.get(event.getPlayer().getName());
			if (platform != null) {
				Player player = event.getPlayer();
				if (isExpired(player)) {
					turnOff(player);
				} else {
					if (falling.contains(player)) {
						if (event.getTo().getY() < event.getFrom().getY()) {
							falling.remove(player);
						} else {
							return;
						}
					}
					if (!player.isSneaking()) { 
						Block block = event.getTo().subtract(0,1,0).getBlock();
						boolean moved = platform.isMoved(block, false);
						if (moved) {
							platform.movePlatform(block, true);
							addUse(player);
							chargeUseCost(player);
						}
					}
				}
			}
		}
	
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
			if (windwalkers.containsKey(event.getPlayer().getName()) && event.isSneaking()) {
				Player player = event.getPlayer();
				if (isExpired(player)) {
					turnOff(player);
				} else {
					Block block = player.getLocation().subtract(0,2,0).getBlock();
					boolean moved = windwalkers.get(player.getName()).movePlatform(block);
					if (moved) {
						falling.add(player);
						addUse(player);
						chargeUseCost(player);
					}
				}			
			}
		}
	
		@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
		public void onBlockBreak(BlockBreakEvent event) {
			if (windwalkers.size() > 0 && event.getBlock().getType() == platformBlock) {
				for (BlockPlatform platform : windwalkers.values()) {
					if (platform.blockInPlatform(event.getBlock())) {
						event.setCancelled(true);
						break;
					}
				}
			}
		}
		
	}

	public class TeleportListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerTeleport(PlayerTeleportEvent event) {
			if (windwalkers.containsKey(event.getPlayer().getName())) {
				if (!event.getFrom().getWorld().getName().equals(event.getTo().getWorld().getName()) || event.getFrom().toVector().distanceSquared(event.getTo().toVector()) > 50*50) {
					turnOff(event.getPlayer());
				}
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerPortal(PlayerPortalEvent event) {
			if (windwalkers.containsKey(event.getPlayer().getName())) {
				turnOff(event.getPlayer());
			}
		}
	}
	
	@Override
	public void turnOffBuff(Player player) {
		BlockPlatform platform = windwalkers.get(player.getName());
		if (platform != null) {
			platform.destroyPlatform();
			windwalkers.remove(player.getName());
			unregisterListener();
		}
	}
	
	@Override
	protected void turnOff() {
		for (BlockPlatform platform : windwalkers.values()) {
			platform.destroyPlatform();
		}
		windwalkers.clear();
		unregisterListener();
	}

	@Override
	public boolean isActive(Player player) {
		return windwalkers.containsKey(player.getName());
	}

}
