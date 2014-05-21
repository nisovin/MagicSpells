package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class CrippleSpell extends TargetedSpell implements TargetedEntitySpell {

	private int strength;
	private int duration;
	
	public CrippleSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strength = getConfigInt("effect-strength", 5);
		duration = getConfigInt("effect-duration", 100);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {		
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				// fail
				return noTarget(player);
			}
			playSpellEffects(player, target.getTarget());
			target.getTarget().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration*target.getPower()), strength), true);
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			playSpellEffects(caster, target);
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration*power), strength), true);
			return true;
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) {
			return false;
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration*power), strength), true);
			return true;
		}
	}

}
