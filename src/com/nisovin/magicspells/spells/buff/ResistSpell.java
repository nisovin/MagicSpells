package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ResistSpell extends BuffSpell {

	List<String> spellDamageTypes;
	List<DamageCause> normalDamageTypes;
	float multiplier;	
	
	Map<String, Float> buffed = new HashMap<String, Float>();
	
	public ResistSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		spellDamageTypes = getConfigStringList("spell-damage-types", null);
		List<String> list = getConfigStringList("normal-damage-types", null);
		multiplier = getConfigFloat("multiplier", 0.5F);
		
		if (list != null) {
			for (String s : list) {
				for (DamageCause cause : DamageCause.values()) {
					if (cause.name().equalsIgnoreCase(s)) {
						normalDamageTypes.add(cause);
						break;
					}
				}
			}
		}
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		buffed.put(player.getName(), power);
		return true;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent event) {
		if (spellDamageTypes != null && event.getSpell() instanceof SpellDamageSpell && event.getTarget() instanceof Player && isActive((Player)event.getTarget())) {
			SpellDamageSpell spell = (SpellDamageSpell)event.getSpell();
			if (spell.getSpellDamageType() != null && spellDamageTypes.contains(spell.getSpellDamageType())) {
				float power = multiplier;
				if (multiplier < 1) {
					power *= (1 / buffed.get(event.getCaster().getName()));
				} else if (multiplier > 1) {
					power *= buffed.get(event.getCaster().getName());
				}
				event.increasePower(power);
				addUseAndChargeCost((Player)event.getTarget());
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (normalDamageTypes != null && normalDamageTypes.contains(event.getCause()) && event.getEntity() instanceof Player && isActive((Player)event.getEntity())) {
			event.setDamage(event.getDamage() * multiplier);
			addUseAndChargeCost((Player)event.getEntity());
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
