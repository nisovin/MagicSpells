package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SwitchHealthSpell extends TargetedSpell implements TargetedEntitySpell {

	public SwitchHealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			}
			boolean ok = switchHealth(player, target);
			if (!ok) {
				return noTarget(player);
			}
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	boolean switchHealth(Player caster, LivingEntity target) {
		if (caster.isDead() || target.isDead()) return false;
		double casterPct = caster.getHealth() / caster.getMaxHealth();
		double targetPct = target.getHealth() / target.getMaxHealth();
		caster.setHealth(targetPct * caster.getMaxHealth());
		target.setHealth(casterPct * target.getMaxHealth());
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		}
		return switchHealth(caster, target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
