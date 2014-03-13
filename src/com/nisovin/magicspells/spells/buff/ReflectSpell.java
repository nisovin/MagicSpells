package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ReflectSpell extends BuffSpell {

	private HashSet<String> reflectors;
	
	public ReflectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		reflectors = new HashSet<String>();
	}
	
	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		reflectors.add(player.getName());
		return true;
	}

	@EventHandler
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.isCancelled()) return;
		if (event.getTarget() instanceof Player) {
			Player target = (Player)event.getTarget();
			if (isActive(target)) {
				boolean ok = chargeUseCost(target);
				if (ok) {
					event.setTarget(event.getCaster());
					addUse(target);
				}
			}
		}
	}

	@Override
	public void turnOffBuff(Player player) {
		reflectors.remove(player.getName());
	}

	@Override
	protected void turnOff() {
		reflectors.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return reflectors.contains(player.getName());
	}

}
