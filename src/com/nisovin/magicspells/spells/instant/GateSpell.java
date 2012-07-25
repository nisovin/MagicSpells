package com.nisovin.magicspells.spells.instant;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;

public class GateSpell extends InstantSpell {
	
	private String world;
	private String coords;
	private String strGateFailed;

	public GateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		world = getConfigString("world", "CURRENT");
		coords = getConfigString("coordinates", "SPAWN");
		strGateFailed = getConfigString("str-gate-failed", "Unable to teleport.");
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
			coords = coords.replace(" ", "");
			if (coords.matches("^-?[0-9]+,[0-9]+,-?[0-9]+(,-?[0-9.]+,-?[0-9.]+)?$")) {
				String[] c = coords.split(",");
				int x = Integer.parseInt(c[0]);
				int y = Integer.parseInt(c[1]);
				int z = Integer.parseInt(c[2]);
				float yaw = 0;
				float pitch = 0;
				if (c.length > 3) {
					yaw = Float.parseFloat(c[3]);
					pitch = Float.parseFloat(c[4]);
				}
				location = new Location(world, x, y, z, yaw, pitch);
			} else if (coords.equals("SPAWN")) {
				location = world.getSpawnLocation();
				location = new Location(world, location.getX(), world.getHighestBlockYAt(location), location.getZ());
			} else if (coords.equals("EXACTSPAWN")) {
				location = world.getSpawnLocation();
			} else if (coords.equals("CURRENT")) {
				Location l = player.getLocation();
				location = new Location(world, l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getYaw(), l.getPitch());
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
			Location from = player.getLocation();
			Location to = b.getLocation();
			boolean teleported = player.teleport(location);
			if (teleported) {
				playSpellEffects(EffectPosition.CASTER, from);
				playSpellEffects(EffectPosition.TARGET, to);
			} else {
				// fail - teleport blocked
				MagicSpells.error(name + ": teleport prevented!");
				sendMessage(player, strGateFailed);
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
