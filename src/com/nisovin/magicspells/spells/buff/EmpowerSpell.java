package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class EmpowerSpell extends BuffSpell {

	private float extraPower;
	
	private HashMap<Player,Float> empowered;
	
	public EmpowerSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		extraPower = getConfigFloat("power-multiplier", 1.5F);
		
		empowered = new HashMap<Player,Float>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (empowered.containsKey(player)) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			float p = power * extraPower;
			empowered.put(player, p);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onSpellCast(SpellCastEvent event) {
		Player player = event.getCaster();
		if (empowered.containsKey(player)) {
			event.increasePower(empowered.get(player));
			addUseAndChargeCost(player);
		}
	}
	
	@Override
	public void turnOff(Player player) {
		if (empowered.containsKey(player)) {
			super.turnOff(player);
			empowered.remove(player);
			sendMessage(player, strFade);
		}
	}

	@Override
	protected void turnOff() {
		empowered.clear();
	}

}
