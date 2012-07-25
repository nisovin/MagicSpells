package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PhaseSpell extends InstantSpell {

	private int range;
	private int maxDistance;
	private List<Integer> allowedPassThru;
	private String strCantPhase;
	
	public PhaseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 5);
		maxDistance = getConfigInt("max-distance", 15);
		allowedPassThru = getConfigIntList("allowed-pass-thru-blocks", null);
		strCantPhase = getConfigString("str-cant-phase", "Unable to find place to phase to.");
		
		if (allowedPassThru != null && allowedPassThru.size() == 0) {
			allowedPassThru = null;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.range * power);
			int distance = Math.round(maxDistance * power);
			
			BlockIterator iter;
			try {
				iter = new BlockIterator(player, distance*2);
			} catch (IllegalStateException e) {
				sendMessage(player, strCantPhase);
				return PostCastAction.ALREADY_HANDLED;
			}
			
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
			
			// find landing spot
			if (start != null) {
				if (allowedPassThru != null && !allowedPassThru.contains(start.getTypeId())) {
					// can't phase through the block
					location = null;
				} else {
					// get next empty space
					Block end = null;
					while (end == null && i++ < distance*2 && iter.hasNext()) {
						Block b = iter.next();
						// check for suitable landing location
						if (b.getType() == Material.AIR && b.getRelative(0, 1, 0).getType() == Material.AIR && player.getLocation().distanceSquared(b.getLocation()) < distance*distance) {
							location = b.getLocation();
							break;
						}
						// check for invalid pass-thru block
						if (allowedPassThru != null && !allowedPassThru.contains(b.getTypeId())) {
							location = null;
							break;
						}
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
			playSpellEffects(EffectPosition.CASTER, player.getLocation());
			playSpellEffects(EffectPosition.TARGET, location);
			player.teleport(location);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
