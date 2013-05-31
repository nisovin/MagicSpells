package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class WindwalkSpell extends BuffSpell {

	private int launchSpeed;
	private float flySpeed;
	private int maxY;
	private int maxAltitude;
    private boolean cancelOnLand;
	private boolean cancelOnTeleport;
	
	private HashSet<Player> flyers;
	private HashMap<Player, Integer> tasks;
	private HeightMonitor heightMonitor = null;
	
	public WindwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		launchSpeed = getConfigInt("launch-speed", 1);
		flySpeed = getConfigFloat("fly-speed", 0.1F);
		maxY = getConfigInt("max-y", 260);
		maxAltitude = getConfigInt("max-altitude", 100);
        cancelOnLand = getConfigBoolean("cancel-on-land", true);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", true);
		
		flyers = new HashSet<Player>();
		if (useCostInterval > 0) {
			tasks = new HashMap<Player, Integer>();
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (cancelOnLand) {
			registerEvents(new SneakListener());
		}
		if (cancelOnLogout) {
			registerEvents(new QuitListener());
		}
		if (cancelOnTeleport) {
			registerEvents(new TeleportListener());
		}
	}

	@Override
	public PostCastAction castSpell(final Player player, SpellCastState state, float power, String[] args) {
		if (flyers.contains(player)) {
			turnOff(player);
			if (toggle) {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		if (state == SpellCastState.NORMAL) {
			// set flying
			flyers.add(player);
			player.setAllowFlight(true);
			player.setFlySpeed(flySpeed);
			if (launchSpeed > 0) {
				player.teleport(player.getLocation().add(0, .25, 0));
				player.setVelocity(new Vector(0,launchSpeed,0));
			}
			player.setFlying(true);
			// set cost interval
			if (useCostInterval > 0 || numUses > 0) {
				int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						addUseAndChargeCost(player);
					}
				}, useCostInterval, useCostInterval);
				tasks.put(player, taskId);
			}
			// start height monitor
			if (heightMonitor == null && (maxY > 0 || maxAltitude > 0)) {
				heightMonitor = new HeightMonitor();
			}
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
    
	public class SneakListener implements Listener {
	    @EventHandler(priority=EventPriority.MONITOR)
	    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
	        if (flyers.contains(event.getPlayer())) {
	            if (event.getPlayer().getLocation().subtract(0,1,0).getBlock().getType() != Material.AIR) {
	                turnOff(event.getPlayer());
	            }
	        }
	    }
	}

	public class TeleportListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerTeleport(PlayerTeleportEvent event) {
			if (flyers.contains(event.getPlayer())) {
				if (!event.getFrom().getWorld().getName().equals(event.getTo().getWorld().getName()) || event.getFrom().toVector().distanceSquared(event.getTo().toVector()) > 50*50) {
					turnOff(event.getPlayer());
				}
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerPortal(PlayerPortalEvent event) {
			if (flyers.contains(event.getPlayer())) {
				turnOff(event.getPlayer());
			}
		}
	}
	
	public class HeightMonitor implements Runnable {
		
		int taskId;
		
		public HeightMonitor() {
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 20, 20);
		}
		
		public void run() {
			for (Player p : flyers) {
				if (maxY > 0) {
					int ydiff = p.getLocation().getBlockY() - maxY;
					if (ydiff > 0) {
						p.setVelocity(p.getVelocity().setY(-ydiff * 1.5));
						continue;
					}
				}
				if (maxAltitude > 0) {
					int ydiff = p.getLocation().getBlockY() - p.getWorld().getHighestBlockYAt(p.getLocation()) - maxAltitude;
					if (ydiff > 0) {
						p.setVelocity(p.getVelocity().setY(-ydiff * 1.5));
					}
				}				
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(taskId);
		}
	}

	@Override
	public void turnOff(final Player player) {
		if (flyers.contains(player)) {
			super.turnOff(player);
			player.setFlying(false);
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setAllowFlight(false);
			}
			player.setFlySpeed(0.1F);
			player.setFallDistance(0);
			flyers.remove(player);
			sendMessage(player, strFade);
		}
		if (tasks != null && tasks.containsKey(player)) {
			int taskId = tasks.remove(player);
			Bukkit.getScheduler().cancelTask(taskId);
		}
		if (heightMonitor != null && flyers.size() == 0) {
			heightMonitor.stop();
			heightMonitor = null;
		}
	}
	
	@Override
	protected void turnOff() {
		HashSet<Player> flyers = new HashSet<Player>(this.flyers);
		for (Player player : flyers) {
			turnOff(player);
		}
		this.flyers.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return flyers.contains(player);
	}

}
