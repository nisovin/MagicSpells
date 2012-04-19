package com.nisovin.magicspells.spells.instant;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PhaseSpell extends InstantSpell {

	private int range;
	private int maxDistance;
	private String strCantPhase;
	
	public PhaseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 5);
		maxDistance = getConfigInt("max-distance", 15);
		strCantPhase = getConfigString("str-cant-phase", "Unable to find place to phase to.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.range * power);
			int distance = Math.round(maxDistance * power);
			
			BlockIterator iter = new BlockIterator(player, distance*2);
			Block start = null;
			int i = 0;
			Location location = null;
			
			// get wall block
			while (start == null && i++ < range*2 && iter.hasNext()) {
				Block b = iter.next();
				if (b.getType() != Material.AIR && player.getLocation().distanceSquared(b.getLocation()) < range*range) {
					start = b;
					break;
				}
			}
			
			// get next empty space
			if (start != null) {
				Block end = null;
				while (end == null && i++ < distance*2 && iter.hasNext()) {
					Block b = iter.next();
					if (b.getType() == Material.AIR && b.getRelative(0, 1, 0).getType() == Material.AIR && player.getLocation().distanceSquared(b.getLocation()) < distance*distance) {
						location = b.getLocation();
						break;
					}
				}
			}
			
			// check for fail
			if (location == null) {
				// no location to tp to
				sendMessage(player, strCantPhase);
				//fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// teleport
			location.setX(location.getX() + .5);
			location.setZ(location.getZ() + .5);
			location.setPitch(player.getLocation().getPitch());
			location.setYaw(player.getLocation().getYaw());
			playGraphicalEffects(1, player);
			playGraphicalEffects(2, location);
			player.teleport(location);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
