package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;

public class ShadowstepSpell extends TargetedEntitySpell {

	private boolean useSpellEffect;
	private boolean targetPlayers;
	private boolean obeyLos;
	private String strNoTarget;
	private String strNoLandingSpot;
	
	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		useSpellEffect = getConfigBoolean("use-spell-effect", true);
		targetPlayers = getConfigBoolean("target-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
		strNoTarget = getConfigString("str-no-target", "No target found.");
		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.range * power);
			
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail
				sendMessage(player, strNoTarget);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			boolean done = shadowstep(player, target);
			if (!done) {
				sendMessage(player, strNoLandingSpot);
				return PostCastAction.ALREADY_HANDLED;
			}
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean shadowstep(Player player, LivingEntity target) {
		// get landing location
		Location targetLoc = target.getLocation();
		Vector facing = targetLoc.getDirection().setY(0).multiply(-1);
		Location loc = targetLoc.toVector().add(facing).toLocation(targetLoc.getWorld());
		loc.setPitch(0);
		loc.setYaw(targetLoc.getYaw());
		
		// check if clear
		Block b = loc.getBlock();
		if (!BlockUtils.isPathable(b.getType()) || !BlockUtils.isPathable(b.getRelative(BlockFace.UP))) {
			// fail - no landing spot
			return false;
		}
		
		// ok
		if (useSpellEffect) {
			player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
			player.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
		}
		player.teleport(loc);
		
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player && !targetPlayers) {
			return false;
		} else {
			return shadowstep(caster, target);
		}
	}

}
