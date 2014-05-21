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
import com.nisovin.magicspells.util.Util;

public class PotionEffectSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private PotionEffectType type;
	private int duration;
	private int strength;
	private boolean ambient;
	private boolean targeted;
	private boolean spellPowerAffectsDuration;
	private boolean spellPowerAffectsStrength;
	
	public PotionEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		type = Util.getPotionEffectType(getConfigString("type", "1"));
		duration = getConfigInt("duration", 0);
		strength = getConfigInt("strength", 0);
		ambient = getConfigBoolean("ambient", false);
		targeted = getConfigBoolean("targeted", false);
		spellPowerAffectsDuration = getConfigBoolean("spell-power-affects-duration", true);
		spellPowerAffectsStrength = getConfigBoolean("spell-power-affects-strength", true);
	}
	
	@Deprecated
	public int getType() {
		return type.getId();
	}
	
	public PotionEffectType getPotionType() {
		return type;
	}
	
	public int getDuration() {
		return duration;
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = null;
			if (targeted) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
				if (targetInfo != null) {
					target = targetInfo.getTarget();
					power = targetInfo.getPower();
				}
			} else {
				target = player;
			}
			if (target == null) {
				// fail no target
				return noTarget(player);
			}
			
			int dur = spellPowerAffectsDuration ? Math.round(duration * power) : duration;
			int str = spellPowerAffectsStrength ? Math.round(strength * power) : strength;
			
			target.addPotionEffect(new PotionEffect(type, dur, str, ambient), true);
			if (targeted) {
				playSpellEffects(player, target);
			} else {
				playSpellEffects(EffectPosition.CASTER, player);
			}
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			int dur = spellPowerAffectsDuration ? Math.round(duration * power) : duration;
			int str = spellPowerAffectsStrength ? Math.round(strength * power) : strength;
			PotionEffect effect = new PotionEffect(type, dur, str, ambient);
			if (targeted) {
				target.addPotionEffect(effect, true);
				playSpellEffects(caster, target);
			} else {
				caster.addPotionEffect(effect, true);
				playSpellEffects(EffectPosition.CASTER, caster);
			}
			return true;
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) {
			return false;
		} else {
			int dur = spellPowerAffectsDuration ? Math.round(duration * power) : duration;
			int str = spellPowerAffectsStrength ? Math.round(strength * power) : strength;
			PotionEffect effect = new PotionEffect(type, dur, str, ambient);
			target.addPotionEffect(effect);
			playSpellEffects(EffectPosition.TARGET, target);
			return true;
		}			
	}

}
