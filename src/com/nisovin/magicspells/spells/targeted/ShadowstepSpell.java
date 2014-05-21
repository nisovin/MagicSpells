package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class ShadowstepSpell extends TargetedSpell implements TargetedEntitySpell {

	private String strNoLandingSpot;
	
	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strNoLandingSpot = getConfigString("str-no-landing-spot", "Cannot shadowstep there.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				// fail
				return noTarget(player);
			}
			
			boolean done = shadowstep(player, target.getTarget());
			if (!done) {
				return noTarget(player, strNoLandingSpot);
			} else {
				sendMessages(player, target.getTarget());
				return PostCastAction.NO_MESSAGES;
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
		playSpellEffects(player.getLocation(), loc);
		player.teleport(loc);
		
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			return shadowstep(caster, target);
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
