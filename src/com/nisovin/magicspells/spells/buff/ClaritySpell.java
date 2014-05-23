package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ClaritySpell extends BuffSpell {

	float multiplier;
	Set<String> spells;
	
	Map<String, Float> buffed = new HashMap<String, Float>();
	
	public ClaritySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		multiplier = getConfigFloat("multiplier", 0.5F);
		List<String> list = getConfigStringList("spells", null);
		if (list != null && list.size() > 0) {
			spells = new HashSet<String>();
			spells.addAll(list);
		}
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		buffed.put(player.getName(), power);
		return true;
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellCast(SpellCastEvent event) {
		if (buffed.containsKey(event.getCaster().getName()) && (spells == null || spells.contains(event.getSpell().getInternalName()))) {
			float power = buffed.get(event.getCaster().getName());
			float mod = multiplier;
			if (multiplier < 1) {
				mod *= (1/power);
			} else if (multiplier > 1) {
				mod *= power;
			}
			event.setReagents(event.getReagents().multiply(mod));
			addUseAndChargeCost(event.getCaster());
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
