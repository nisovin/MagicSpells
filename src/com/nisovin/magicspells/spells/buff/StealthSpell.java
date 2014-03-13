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
	public boolean castBuff(Player player, float power, String[] args) {
		stealthy.add(player.getName());
		return true;
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
	public void turnOffBuff(Player player) {
		stealthy.remove(player.getName());
	}
	
	@Override
	protected void turnOff() {
		stealthy.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return stealthy.contains(player.getName());
	}
	
}