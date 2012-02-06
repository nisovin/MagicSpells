package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;

public class ShadowstepSpell extends TargetedSpell {

	private boolean useSpellEffect;
	private boolean targetPlayers;
	private boolean obeyLos;
	
	public ShadowstepSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		useSpellEffect = getConfigBoolean("use-spell-effect", true);
		targetPlayers = getConfigBoolean("target-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.range * power);
			
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail
				player.sendMessage("No target");
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get landing location
			Location targetLoc = target.getLocation();
			Vector facing = targetLoc.getDirection().setY(0).multiply(-1);
			Location loc = targetLoc.toVector().add(facing).toLocation(targetLoc.getWorld());
			loc.setPitch(0);
			loc.setYaw(targetLoc.getYaw());
			
			// check if clear
			Block b = loc.getBlock();
			if (!BlockUtils.isPathable(b.getType()) || !BlockUtils.isPathable(b.getRelative(BlockFace.UP))) {
				// fail
				player.sendMessage("No landing spot");
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// ok
			if (useSpellEffect) {
				player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
				player.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
			}
			player.teleport(loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
