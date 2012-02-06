package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTargetEvent;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class StealthSpell extends BuffSpell {
	
	private HashSet<String> stealthy;
	
	public StealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		stealthy = new HashSet<String>();
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (stealthy.contains(player.getName())) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			stealthy.add(player.getName());
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if (!event.isCancelled() && stealthy.size() > 0 && event.getTarget() instanceof Player) {
			Player player = (Player)event.getTarget();
			if (stealthy.contains(player.getName())) {
				if (isExpired(player)) {
					turnOff(player);
				} else {
					addUse(player);
					boolean ok = chargeUseCost(player);
					if (ok) {
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	@Override
	public void turnOff(Player player) {
		if (stealthy.contains(player.getName())) {
			super.turnOff(player);
			stealthy.remove(player.getName());
			sendMessage(player, strFade);
		}
	}
	
	@Override
	protected void turnOff() {
		stealthy.clear();
	}
	
}