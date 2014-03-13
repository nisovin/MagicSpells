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
	
	private HashSet<String> flyers;
	private HashMap<String, Integer> tasks;
	private HeightMonitor heightMonitor = null;
	
	public WindwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		launchSpeed = getConfigInt("launch-speed", 1);
		flySpeed = getConfigFloat("fly-speed", 0.1F);
		maxY = getConfigInt("max-y", 260);
		maxAltitude = getConfigInt("max-altitude", 100);
        cancelOnLand = getConfigBoolean("cancel-on-land", true);
		
		flyers = new HashSet<String>();
		if (useCostInterval > 0) {
			tasks = new HashMap<String, Integer>();
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (cancelOnLand) {
			registerEvents(new SneakListener());
		}
	}

	@Override
	public boolean castBuff(final Player player, float power, String[] args) {
		// set flying
		if (launchSpeed > 0) {
			player.teleport(player.getLocation().add(0, .25, 0));
			player.setVelocity(new Vector(0,launchSpeed,0));
		}
		flyers.add(player.getName());
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setFlySpeed(flySpeed);
		// set cost interval
		if (useCostInterval > 0 || numUses > 0) {
			int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					addUseAndChargeCost(player);
				}
			}, useCostInterval, useCostInterval);
			tasks.put(player.getName(), taskId);
		}
		// start height monitor
		if (heightMonitor == null && (maxY > 0 || maxAltitude > 0)) {
			heightMonitor = new HeightMonitor();
		}
		return true;
	}
    
	public class SneakListener implements Listener {
	    @EventHandler(priority=EventPriority.MONITOR)
	    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
	        if (flyers.contains(event.getPlayer().getName())) {
	            if (event.getPlayer().getLocation().subtract(0,1,0).getBlock().getType() != Material.AIR) {
	                turnOff(event.getPlayer());
	            }
	        }
	    }
	}
	
	public class HeightMonitor implements Runnable {
		
		int taskId;
		
		public HeightMonitor() {
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 20, 20);
		}
		
		public void run() {
			for (String name : flyers) {
				Player p = Bukkit.getPlayerExact(name);
				if (p != null && p.isValid()) {
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
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(taskId);
		}
	}

	@Override
	public void turnOffBuff(final Player player) {
		if (flyers.remove(player.getName())) {
			player.setFlying(false);
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setAllowFlight(false);
			}
			player.setFlySpeed(0.1F);
			player.setFallDistance(0);
		}
		if (tasks != null && tasks.containsKey(player.getName())) {
			int taskId = tasks.remove(player.getName());
			Bukkit.getScheduler().cancelTask(taskId);
		}
		if (heightMonitor != null && flyers.size() == 0) {
			heightMonitor.stop();
			heightMonitor = null;
		}
	}
	
	@Override
	protected void turnOff() {
		HashSet<String> flyers = new HashSet<String>(this.flyers);
		for (String name : flyers) {
			Player player = Bukkit.getPlayerExact(name);
			if (player != null) {
				turnOff(player);
			}
		}
		this.flyers.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return flyers.contains(player.getName());
	}

}
