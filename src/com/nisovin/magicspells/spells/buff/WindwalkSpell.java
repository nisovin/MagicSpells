package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class WindwalkSpell extends BuffSpell {

	private int launchSpeed;
    private boolean cancelOnLand;
	private boolean cancelOnLogout;
	private boolean cancelOnTeleport;
	
	private HashSet<Player> flyers;
	private HashMap<Player, Integer> tasks;
	
	public WindwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		launchSpeed = getConfigInt("launch-speed", 1);
        cancelOnLand = getConfigBoolean("cancel-on-land", true);
		cancelOnLogout = getConfigBoolean("cancel-on-logout", true);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", true);
		
		flyers = new HashSet<Player>();
		if (useCostInterval > 0) {
			tasks = new HashMap<Player, Integer>();
		}
	}

	@Override
	public PostCastAction castSpell(final Player player, SpellCastState state, float power, String[] args) {
		if (flyers.contains(player)) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			// set flying
			flyers.add(player);
			player.setAllowFlight(true);
			player.setFlying(true);
			if (launchSpeed > 0) {
				player.setVelocity(new Vector(0,launchSpeed,0));
			}
			// set duration limit
			if (duration > 0) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						turnOff(player);
					}
				}, duration*20);
			}
			// set cost interval
			if (useCostInterval > 0) {
				int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						addUseAndChargeCost(player);
					}
				}, useCostInterval*20, useCostInterval*20);
				tasks.put(player, taskId);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
    
    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (cancelOnLand && flyers.contains(event.getPlayer())) {
            if (event.getPlayer().getLocation().subtract(0,1,0).getBlock().getType() != Material.AIR) {
                turnOff(event.getPlayer());
            }
        }
    }

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (cancelOnLogout) {
			turnOff(event.getPlayer());
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.isCancelled()) return;
		if (cancelOnTeleport && flyers.contains(event.getPlayer())) {
			if (!event.getFrom().getWorld().getName().equals(event.getTo().getWorld().getName()) || event.getFrom().toVector().distanceSquared(event.getTo().toVector()) > 50*50) {
				turnOff(event.getPlayer());
			}
		}
	}

	@Override
	public void turnOff(final Player player) {
		super.turnOff(player);
		if (flyers.contains(player)) {
			player.setFlying(false);
			player.setAllowFlight(false);
			flyers.remove(player);
			sendMessage(player, strFade);
		}
		if (tasks != null && tasks.containsKey(player)) {
			int taskId = tasks.remove(player);
			Bukkit.getScheduler().cancelTask(taskId);
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

}
