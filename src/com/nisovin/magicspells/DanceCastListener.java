package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.Util;

public class DanceCastListener implements Listener {

	MagicSpells plugin;
	
	CastItem danceCastWand;
	int duration;
	Map<String, Spell> spells = new HashMap<String, Spell>();
	
	Map<String, String> playerCasts = new HashMap<String, String>();
	Map<String, Location> playerLocations = new HashMap<String, Location>();
	Map<String, Integer> playerTasks = new HashMap<String, Integer>();
	
	boolean enableDoubleJump = false;
	boolean enableMovement = false;
	
	public DanceCastListener(MagicSpells plugin, String castItem, int duration) {
		this.plugin = plugin;
		
		this.danceCastWand = new CastItem(Util.getItemStackFromString(castItem));
		this.duration = duration;
		
		for (Spell spell : MagicSpells.spells()) {
			String seq = spell.getDanceCastSequence();
			if (seq != null && seq.matches("[CSUJLRFB]+")) {
				spells.put(seq, spell);
				if (seq.contains("J")) enableDoubleJump = true;
				if (seq.contains("F") || seq.contains("B") || seq.contains("L") || seq.contains("R")) enableMovement = true;
				MagicSpells.debug("Dance cast registered: " + spell.getInternalName() + " - " + seq);
			}
		}
		
		if (spells.size() > 0) {
			MagicSpells.registerEvents(this);
		}
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (!event.hasItem()) return;
		if (!danceCastWand.equals(event.getItem())) return;
		
		Action action = event.getAction();
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			String castSequence = playerCasts.remove(playerName);
			if (castSequence != null) {
				// finished casting - cast the spell
				if (enableDoubleJump) {
					player.setFlying(false);
					player.setAllowFlight(false);
				}
				castSequence = processMovement(player, castSequence);
				MagicSpells.debug("Player " + player.getName() + " performed dance sequence " + castSequence);
				Spell spell = spells.get(castSequence);
				if (spell != null) {
					MagicSpells.sendMessage(player, plugin.strDanceComplete);
					spell.cast(player);
				} else {
					MagicSpells.sendMessage(player, plugin.strDanceFail);
				}
				playerLocations.remove(playerName);
				Integer taskId = playerTasks.remove(playerName);
				if (taskId != null) {
					MagicSpells.cancelTask(taskId.intValue());
				}
			} else {
				// starting a cast
				if (!player.isSneaking() && !player.isFlying()) {
					playerCasts.put(playerName, "");
					playerLocations.put(playerName, player.getLocation());
					if (enableDoubleJump) {
						player.setAllowFlight(true);
						player.setFlying(false);
					}
					MagicSpells.sendMessage(player, plugin.strDanceStart);
					if (duration > 0) {
						playerTasks.put(playerName, MagicSpells.scheduleDelayedTask(new DanceCastDuration(playerName), duration));
					}
				}
			}
		} else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			String castSequence = playerCasts.get(playerName);
			if (castSequence != null) {
				castSequence = processMovement(player, castSequence);
				playerCasts.put(playerName, castSequence + "C");
			}
		}
	}
	
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		String castSequence = playerCasts.get(playerName);
		if (castSequence != null) {
			castSequence = processMovement(player, castSequence);
			playerCasts.put(playerName, castSequence + (event.isSneaking() ? "S" : "U"));
		}
	}
	
	@EventHandler
	public void onFly(PlayerToggleFlightEvent event) {
		if (!enableDoubleJump) return;
		Player player = event.getPlayer();
		String playerName = player.getName();
		String castSequence = playerCasts.get(playerName);
		if (castSequence != null && event.isFlying()) {
			event.setCancelled(true);
			castSequence = processMovement(player, castSequence);
			playerCasts.put(playerName, castSequence + "J");
		}		
	}
	
	String processMovement(Player player, String castSequence) {
		if (!enableMovement) return castSequence;
		
		Location firstLoc = playerLocations.get(player.getName());
		playerLocations.put(player.getName(), player.getLocation());
		
		if (firstLoc == null || firstLoc.distanceSquared(player.getLocation()) < 0.04F) return castSequence;
		
		float facing = firstLoc.getYaw();
		if (facing < 0) facing += 360;
		float dir = (float)Util.getYawOfVector(player.getLocation().toVector().subtract(firstLoc.toVector()));
		if (dir < 0) dir += 360;		
		float diff = facing - dir;
		if (diff < 0) diff += 360;
		
		if (diff < 20 || diff > 340) {
			return castSequence + "F";
		} else if (70 < diff && diff < 110) {
			return castSequence + "L";
		} else if (160 < diff && diff < 200) {
			return castSequence + "B";
		} else if (250 < diff && diff < 290) {
			return castSequence + "R";
		}		
		
		return castSequence;
	}
	
	public class DanceCastDuration implements Runnable {
		
		String playerName;
		
		public DanceCastDuration(String playerName) {
			this.playerName = playerName;
		}
		
		@Override
		public void run() {
			String cast = playerCasts.remove(playerName);
			playerLocations.remove(playerName);
			playerTasks.remove(playerName);
			if (cast != null) {
				Player player = Bukkit.getPlayerExact(playerName);
				if (player != null) {
					MagicSpells.sendMessage(player, plugin.strDanceFail);
				}
			}
		}
	}
	
}
