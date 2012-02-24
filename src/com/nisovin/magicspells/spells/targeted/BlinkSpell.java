package com.nisovin.magicspells.spells.targeted;

import java.util.HashSet;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;

public class BlinkSpell extends TargetedLocationSpell {
	
	private boolean passThroughCeiling;
	private boolean smokeTrail;
	private boolean portalAnimation;
	private String strCantBlink = null;
	
	public BlinkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		passThroughCeiling = getConfigBoolean("pass-through-ceiling", false);
		smokeTrail = config.getBoolean("spells." + spellName + ".smoke-trail", true);
		portalAnimation = getConfigBoolean("portal-animation", true);
		strCantBlink = config.getString("spells." + spellName + ".str-cant-blink", "You can't blink there.");
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.range*power);
			if (range <= 0) range = 25;
			if (range > 125) range = 125;
			BlockIterator iter = new BlockIterator(player, range>0&&range<150?range:150);
			HashSet<Location> smokes = null;
			if (smokeTrail) {
				smokes = new HashSet<Location>();
			}
			Block prev = null;
			Block found = null;
			Block b;
			while (iter.hasNext()) {
				b = iter.next();
				if (b.getType() == Material.AIR) {
					prev = b;
					if (smokeTrail) {
						smokes.add(b.getLocation());
					}
				} else {
					found = b;
					break;
				}
			}
			
			if (found != null) {
				Location loc = null;
				if (range > 0 && !inRange(found.getLocation(), player.getLocation(), range)) {
				} else if (!passThroughCeiling && found.getRelative(0,-1,0).equals(prev)) {
					// trying to move upward
					if (BlockUtils.isPathable(prev) && BlockUtils.isPathable(prev.getRelative(0,-1,0))) {
						loc = prev.getRelative(0,-1,0).getLocation();
					}
				} else if (BlockUtils.isPathable(found.getRelative(0,1,0)) && BlockUtils.isPathable(found.getRelative(0,2,0))) {
					// try to stand on top
					loc = found.getLocation();
					loc.setY(loc.getY() + 1);
				} else if (prev != null && BlockUtils.isPathable(prev) && BlockUtils.isPathable(prev.getRelative(0,1,0))) {
					// no space on top, put adjacent instead
					loc = prev.getLocation();
				}
				if (loc != null) {
					loc.setX(loc.getX()+.5);
					loc.setZ(loc.getZ()+.5);
					loc.setPitch(player.getLocation().getPitch());
					loc.setYaw(player.getLocation().getYaw());
					teleport(player, loc, smokes);
				} else {
					sendMessage(player, strCantBlink);
					fizzle(player);
					return PostCastAction.ALREADY_HANDLED;
				}
			} else {
				sendMessage(player, strCantBlink);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void teleport(Player player, Location location, HashSet<Location> smokeLocs) {
		if (portalAnimation) {
			location.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 0);
			location.getWorld().playEffect(location, Effect.ENDER_SIGNAL, 0);
		}
		player.teleport(location);
		if (smokeTrail && smokeLocs != null) {
			for (Location l : smokeLocs) {
				l.getWorld().playEffect(l, Effect.SMOKE, 4);
			}
		}
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		target.setYaw(caster.getLocation().getYaw());
		target.setPitch(caster.getLocation().getPitch());
		teleport(caster, target, null);
		return true;
	}

}
