package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class EmpowerSpell extends BuffSpell {

	private float extraPower;
	private float maxPower;
	private Set<String> spells;
	
	private HashMap<String, Float> empowered;
	
	public EmpowerSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		extraPower = getConfigFloat("power-multiplier", 1.5F);
		maxPower = getConfigFloat("max-power-multiplier", 1.5F);
		List<String> list = getConfigStringList("spells", null);
		if (list != null && list.size() > 0) {
			spells = new HashSet<String>(list);
		}
		
		empowered = new HashMap<String, Float>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		float p = power * extraPower;
		if (p > maxPower) p = maxPower;
		empowered.put(player.getName(), p);
		return true;
	}
	
	@Override
	public boolean recastBuff(Player player, float power, String[] args) {
		if (maxPower > extraPower) {
			float p = empowered.get(player.getName());
			p += power * extraPower;
			if (p > maxPower) p = maxPower;
			empowered.put(player.getName(), p);
		}
		return true;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onSpellCast(SpellCastEvent event) {
		Player player = event.getCaster();
		if (player != null && empowered.containsKey(player.getName()) && (spells == null || spells.contains(event.getSpell().getInternalName()))) {
			event.increasePower(empowered.get(player.getName()));
			addUseAndChargeCost(player);
		}
	}
	
	@Override
	public void turnOffBuff(Player player) {
		empowered.remove(player.getName());
	}

	@Override
	protected void turnOff() {
		empowered.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return empowered.containsKey(player.getName());
	}

}
