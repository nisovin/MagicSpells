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

	int regenModAmt;

	private HashSet<Player> regenning;

	public ManaRegenSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		regenModAmt = getConfigInt("regen-mod-amt", 3);
		regenning = new HashSet<Player>();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if(regenning.contains(player)){
			turnOff(player);
		}
		if (state == SpellCastState.NORMAL) {
			regenning.add(player);
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;  
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onManaRegenTick(ManaChangeEvent event) {
		Player p = event.getPlayer();
		if(!isExpired(p) && regenning.contains(p) && event.getReason().equals(ManaChangeReason.REGEN)) {
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
	public void turnOff(Player player) {
		if(regenning.contains(player)){
			super.turnOff(player);
			regenning.remove(player);
			sendMessage(player, strFade);
		}
	}

	@Override
	protected void turnOff() {
		regenning.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return regenning.contains(player);
	}

}
