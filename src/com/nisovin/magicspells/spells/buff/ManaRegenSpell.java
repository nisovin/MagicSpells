package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ManaRegenSpell extends BuffSpell { 

	private int regenModAmt;

	private HashSet<String> regenning;

	public ManaRegenSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		regenModAmt = getConfigInt("regen-mod-amt", 3);
		regenning = new HashSet<String>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		regenning.add(player.getName());
		return true;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onManaRegenTick(ManaChangeEvent event) {
		Player p = event.getPlayer();
		if(!isExpired(p) && isActive(p) && event.getReason().equals(ManaChangeReason.REGEN)) {
			int newAmt = event.getNewAmount() + regenModAmt;
			if (newAmt > event.getMaxMana()) {
				newAmt = event.getMaxMana();
			} else if (newAmt < 0) {
				newAmt = 0;
			}
			event.setNewAmount(newAmt);
			addUseAndChargeCost(p);
		}
	}

	@Override
	public void turnOffBuff(Player player) {
		regenning.remove(player.getName());
	}

	@Override
	protected void turnOff() {
		regenning.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return regenning.contains(player.getName());
	}

}
