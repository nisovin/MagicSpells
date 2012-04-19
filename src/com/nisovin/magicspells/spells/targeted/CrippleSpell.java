package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class CrippleSpell extends TargetedEntitySpell {

	private int strength;
	private int duration;
	private boolean targetPlayers;
	private boolean obeyLos;
	
	public CrippleSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strength = getConfigInt("effect-strength", 5);
		duration = getConfigInt("effect-duration", 100);
		targetPlayers = getConfigBoolean("target-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {		
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail
				sendMessage(player, strNoTarget);
				fizzle(player);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
			playGraphicalEffects(player, target);
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration*power), strength), true);
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player && !targetPlayers) {
			return false;
		} else {
			playGraphicalEffects(caster, target);
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration*power), strength), true);
			return true;
		}
	}

}
