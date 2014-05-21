package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class TeleportSpell extends TargetedSpell implements TargetedEntitySpell {

	public TeleportSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			}
			boolean ok = teleport(player, target.getTarget());
			if (!ok) {
				return noTarget(player);
			}
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	boolean teleport(Player caster, LivingEntity target) {
		Location casterLoc = caster.getLocation();
		boolean ok = caster.teleport(target);
		if (ok) {
			playSpellEffects(EffectPosition.CASTER, casterLoc);
			playSpellEffects(EffectPosition.TARGET, target.getLocation());
			playSpellEffectsTrail(casterLoc, target.getLocation());
		}
		return ok;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return teleport(caster, target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
