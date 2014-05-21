package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ResistSpell extends BuffSpell {

	String spellDamageType;
	float powerMultiplier;
	
	Map<String, Float> buffed = new HashMap<String, Float>();
	
	public ResistSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		spellDamageType = getConfigString("spell-damage-type", "");
		powerMultiplier = getConfigFloat("power-multiplier", 0.5F);
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		buffed.put(player.getName(), power);
		return true;
	}
	
	@EventHandler
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.getSpell() instanceof SpellDamageSpell && isActive(event.getCaster())) {
			SpellDamageSpell spell = (SpellDamageSpell)event.getSpell();
			if (spell.getSpellDamageType() != null && spell.getSpellDamageType().equals(spellDamageType)) {
				float power = (1 / buffed.get(event.getCaster().getName())) * powerMultiplier;
				event.increasePower(power);
			}
		}
	}

	@Override
	public boolean isActive(Player player) {
		return buffed.containsKey(player.getName());
	}

	@Override
	protected void turnOffBuff(Player player) {
		buffed.remove(player.getName());
	}

	@Override
	protected void turnOff() {
		buffed.clear();
	}

}
