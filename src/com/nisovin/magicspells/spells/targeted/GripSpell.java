package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;

public class GripSpell extends TargetedEntitySpell {

	float locationOffset;
	boolean targetPlayers;
	boolean targetNonPlayers;
	boolean obeyLos;
	
	public GripSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		locationOffset = getConfigFloat("location-offset", 0);
		targetPlayers = getConfigBoolean("target-players", true);
		targetNonPlayers = getConfigBoolean("target-non-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, minRange, range, targetPlayers, targetNonPlayers, obeyLos, true);
			if (target != null) {
				grip(player, target);
				sendMessages(player, target);
				return PostCastAction.NO_MESSAGES;
			} else {
				return noTarget(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void grip(Player player, LivingEntity target) {
		Location loc = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(locationOffset));
		if (!BlockUtils.isSafeToStand(loc)) {
			loc = player.getLocation();
		}
		playSpellEffects(player, target);
		target.teleport(loc);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		grip(caster, target);
		return true;
	}

}
