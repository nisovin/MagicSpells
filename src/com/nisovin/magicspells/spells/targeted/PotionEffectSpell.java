package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PotionEffectSpell extends TargetedSpell {

	@SuppressWarnings("unused")
	private static final String SPELL_NAME = "potion";
	
	private int type;
	private int duration;
	private int amplifier;
	private boolean targeted;
	private boolean targetPlayers;
	private boolean targetNonPlayers;
	private boolean obeyLos;
	private String strNoTarget;
	
	public PotionEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		type = getConfigInt("type", 0);
		duration = getConfigInt("duration", 0);
		amplifier = getConfigInt("strength", 0);
		targeted = getConfigBoolean("targeted", false);
		targetPlayers = getConfigBoolean("target-players", false);
		targetNonPlayers = getConfigBoolean("target-non-players", true);
		obeyLos = getConfigBoolean("obey-los", true);
		strNoTarget = getConfigString("str-no-target", "No target found.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target;
			if (targeted) {
				target = getTargetedEntity(player, range, targetPlayers, targetNonPlayers, obeyLos);
			} else {
				target = player;
			}
			if (target == null) {
				// fail no target
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			setMobEffect(target, type, duration, amplifier);
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}

}
