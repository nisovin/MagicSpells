package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class ModifyCooldownSpell extends TargetedSpell implements TargetedEntitySpell {

	List<Spell> spells;
	List<String> spellNames;
	
	float seconds;
	float multiplier;
	
	public ModifyCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		spellNames = getConfigStringList("spells", null);
		seconds = getConfigFloat("seconds", 1f);
		multiplier = getConfigFloat("multiplier", 0f);
	}
	
	@Override
	public void initialize() {
		spells = new ArrayList<Spell>();
		if (spellNames != null) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null) {
					spells.add(spell);
				} else {
					MagicSpells.error("Invalid spell '" + spellName + "' on ModifyCooldownSpell + '" + internalName + "'");
				}
			}
		}
		if (spells.size() == 0) {
			MagicSpells.error("ModifyCooldownSpell '" + internalName + "' has no spells!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> target = getTargetedPlayer(player, power);
			if (target == null) {
				return noTarget(player);
			}
			modifyCooldowns(target.getTarget(), target.getPower());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	void modifyCooldowns(Player player, float power) {
		float sec = seconds * power;
		float mult = multiplier * (1f / power);
		
		for (Spell spell : spells) {
			float cd = spell.getCooldown(player);
			if (cd > 0) {
				cd -= sec;
				if (mult > 0) cd *= mult;
				if (cd < 0) cd = 0;
				spell.setCooldown(player, cd, false);
			}
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) {
			modifyCooldowns((Player)target, power);
			playSpellEffects(caster, target);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (target instanceof Player) {
			modifyCooldowns((Player)target, power);
			playSpellEffects(EffectPosition.TARGET, target);
			return true;
		} else {
			return false;
		}
	}

}
