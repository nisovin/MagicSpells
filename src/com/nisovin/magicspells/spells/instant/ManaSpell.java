package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ManaSpell extends InstantSpell {

	private int mana;
	
	public ManaSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		mana = getConfigInt("mana", 25);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int amount = Math.round(mana*power);
			boolean added = MagicSpells.getManaHandler().addMana(player, amount, ManaChangeReason.OTHER);
			if (!added) {
				return PostCastAction.ALREADY_HANDLED;
			} else {
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}	

}
