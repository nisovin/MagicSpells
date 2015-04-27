package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;

public class RotateSpell extends TargetedSpell implements TargetedEntitySpell {

	boolean random;
	int rotation;
	
	public RotateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		random = getConfigBoolean("random", false);
		rotation = getConfigInt("rotation", 10);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			}
			spin(target.getTarget());
			playSpellEffects(player, target.getTarget());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	void spin(LivingEntity target) {
		if (random) {
			Location loc = target.getLocation();
			loc.setYaw(Util.getRandomInt(360));
			target.teleport(loc);
		} else {
			Location loc = target.getLocation();
			loc.setYaw(loc.getYaw() + rotation);
			target.teleport(loc);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		spin(target);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		spin(target);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

}
