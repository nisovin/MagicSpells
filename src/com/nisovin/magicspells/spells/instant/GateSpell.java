package com.nisovin.magicspells.spells.instant;

import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;

public class GateSpell extends InstantSpell {
	
	private String world;
	private String coords;
	private int castTime;
	private String strGateFailed;
	private String strCastDone;
	private String strCastInterrupted;
	
	private HashSet<String> casting;

	public GateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		world = getConfigString("world", "CURRENT");
		coords = getConfigString("coordinates", "SPAWN");
		castTime = getConfigInt("cast-time", 0);
		strGateFailed = getConfigString("str-gate-failed", "Unable to teleport.");
		strCastDone = getConfigString("str-cast-done", "");
		strCastInterrupted = getConfigString("str-cast-interrupted", "");
		
		if (castTime > 0) {
			casting = new HashSet<String>();
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get world
			World world;
			if (this.world.equals("CURRENT")) {
				world = player.getWorld();
			} else if (this.world.equals("DEFAULT")) {
				world = Bukkit.getServer().getWorlds().get(0);
			} else {
				world = Bukkit.getServer().getWorld(this.world);
			}
			if (world == null) {
				// fail -- no world
				MagicSpells.error(name + ": world " + this.world + " does not exist");
				sendMessage(player, strGateFailed);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get location
			Location location;
			if (coords.matches("^-?[0-9]+, ?[0-9]+, ?-?[0-9]+$")) {
				String[] c = coords.replace(" ", "").split(",");
				int x = Integer.parseInt(c[0]);
				int y = Integer.parseInt(c[1]);
				int z = Integer.parseInt(c[2]);
				location = new Location(world, x, y, z);
			} else if (coords.equals("SPAWN")) {
				location = world.getSpawnLocation();
				location = new Location(world, location.getX(), world.getHighestBlockYAt(location), location.getZ());
			} else if (coords.equals("EXACTSPAWN")) {
				location = world.getSpawnLocation();
			} else if (coords.equals("CURRENT")) {
				Location l = player.getLocation();
				location = new Location(world, l.getBlockX(), l.getBlockY(), l.getBlockZ());
			} else {
				// fail -- no location
				MagicSpells.error(name + ": " + this.coords + " is not a valid location");
				sendMessage(player, strGateFailed);
				return PostCastAction.ALREADY_HANDLED;
			}
			location.setX(location.getX()+.5);
			location.setZ(location.getZ()+.5);
			MagicSpells.debug(3, "Gate location: " + location.toString());
			
			// check for landing point
			Block b = location.getBlock();
			if (!BlockUtils.isPathable(b) || !BlockUtils.isPathable(b.getRelative(0,1,0))) {
				// fail -- blocked
				MagicSpells.error(name + ": landing spot blocked");
				sendMessage(player, strGateFailed);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// teleport caster
			if (castTime > 0) {
				// wait a bit
				casting.add(player.getName());
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Teleporter(player, location), castTime);
			} else {
				// go instantly
				Location from = player.getLocation();
				Location to = b.getLocation();
				boolean teleported = player.teleport(location);
				if (teleported) {
					playGraphicalEffects(1, from);
					playGraphicalEffects(2, to);
				} else {
					// fail - teleport blocked
					MagicSpells.error(name + ": teleport prevented!");
					sendMessage(player, strGateFailed);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		if (castTime <= 0) return;
		
		Entity e = event.getEntity();
		if (e instanceof Player) {
			String name = ((Player)e).getName();
			if (casting.contains(name)) {
				casting.remove(name);
				sendMessage((Player)e, strCastInterrupted);
			}
		}
	}
	
	private class Teleporter implements Runnable {
		private Player player;
		private Location location;
		private Location target;
		
		public Teleporter(Player player, Location target) {
			this.player = player;
			this.location = player.getLocation().clone();
			this.target = target;
		}
		
		public void run() {
			if (casting.contains(player.getName())) {
				casting.remove(player.getName());
				Location loc = player.getLocation();
				if (Math.abs(location.getX()-loc.getX()) < .1 && Math.abs(location.getY()-loc.getY()) < .1 && Math.abs(location.getZ()-loc.getZ()) < .1) {
					boolean teleported = player.teleport(target);
					if (teleported) {
						playGraphicalEffects(1, location);
						playGraphicalEffects(2, target);
						sendMessage(player, strCastDone);
					} else {
						// fail -- teleport prevented
						MagicSpells.error(name + ": teleport prevented!");
						sendMessage(player, strGateFailed);
					}
				} else {
					sendMessage(player, strCastInterrupted);
				}
			}
		}
	}

}
