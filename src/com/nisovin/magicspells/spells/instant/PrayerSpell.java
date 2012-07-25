package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PrayerSpell extends InstantSpell {
	
	private int amountHealed;
	private String strAtFullHealth;

	public PrayerSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		amountHealed = getConfigInt("amount-healed", 10);
		strAtFullHealth = getConfigString("str-at-full-health", "You are already at full health.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (player.getHealth() == 20) {
				sendMessage(player, strAtFullHealth);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				int health = player.getHealth() + Math.round(amountHealed*power);
				if (health > 20) {
					health = 20;
				}
				player.setHealth(health);
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
