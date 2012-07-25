package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PotionEffectSpell extends TargetedEntitySpell {

	@SuppressWarnings("unused")
	private static final String SPELL_NAME = "potion";
	
	private int type;
	private int duration;
	private int amplifier;
	private boolean targeted;
	private boolean targetPlayers;
	private boolean targetNonPlayers;
	private boolean obeyLos;
	private boolean beneficial;
	
	public PotionEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		type = getConfigInt("type", 0);
		duration = getConfigInt("duration", 0);
		amplifier = getConfigInt("strength", 0);
		targeted = getConfigBoolean("targeted", false);
		targetPlayers = getConfigBoolean("target-players", false);
		targetNonPlayers = getConfigBoolean("target-non-players", true);
		obeyLos = getConfigBoolean("obey-los", true);
		beneficial = getConfigBoolean("beneficial", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target;
			if (targeted) {
				target = getTargetedEntity(player, range, targetPlayers, targetNonPlayers, obeyLos, true);
			} else {
				target = player;
			}
			if (target == null) {
				// fail no target
				return noTarget(player);
			}
			
			target.addPotionEffect(new PotionEffect(PotionEffectType.getById(type), Math.round(duration*power), amplifier));
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
		if (target instanceof Player && !targetPlayers) {
			return false;
		} else if (!(target instanceof Player) && !targetNonPlayers) {
			return false;
		} else {
			PotionEffect effect = new PotionEffect(PotionEffectType.getById(type), Math.round(duration*power), amplifier);
			if (targeted) {
				target.addPotionEffect(effect);
				playSpellEffects(caster, target);
			} else {
				caster.addPotionEffect(effect);
				playSpellEffects(EffectPosition.CASTER, caster);
			}
			return true;
		}
	}
	
	@Override
	public boolean isBeneficial() {
		return beneficial;
	}

}
