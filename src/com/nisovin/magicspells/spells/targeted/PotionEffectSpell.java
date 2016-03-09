package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.events.SpellApplyDamageEvent;
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
	private boolean hidden;
	private boolean targeted;
	private boolean spellPowerAffectsDuration;
	private boolean spellPowerAffectsStrength;
	
	public PotionEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		type = Util.getPotionEffectType(getConfigString("type", "1"));
		duration = getConfigInt("duration", 0);
		strength = getConfigInt("strength", 0);
		ambient = getConfigBoolean("ambient", false);
		hidden = getConfigBoolean("hidden", false);
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
			
			applyPotionEffect(player, target, new PotionEffect(type, dur, str, ambient, !hidden));
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
	
	void applyPotionEffect(Player caster, LivingEntity target, PotionEffect effect) {
		DamageCause cause = null;
		if (effect.getType() == PotionEffectType.POISON) {
			cause = DamageCause.POISON;
		} else if (effect.getType() == PotionEffectType.WITHER) {
			cause = DamageCause.WITHER;
		}
		if (cause != null) {
			Bukkit.getPluginManager().callEvent(new SpellApplyDamageEvent(this, caster, target, effect.getAmplifier(), cause, ""));
		}
		target.addPotionEffect(effect, true);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			int dur = spellPowerAffectsDuration ? Math.round(duration * power) : duration;
			int str = spellPowerAffectsStrength ? Math.round(strength * power) : strength;
			PotionEffect effect = new PotionEffect(type, dur, str, ambient, !hidden);
			if (targeted) {
				applyPotionEffect(caster, target, effect);
				playSpellEffects(caster, target);
			} else {
				applyPotionEffect(caster, caster, effect);
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
			PotionEffect effect = new PotionEffect(type, dur, str, ambient, !hidden);
			applyPotionEffect(null, target, effect);
			playSpellEffects(EffectPosition.TARGET, target);
			return true;
		}			
	}

}
