package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class HasteSpell extends BuffSpell {

	private int strength;
	private int boostDuration;
	
	private HashMap<Player,Integer> hasted;
	
	public HasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strength = getConfigInt("effect-strength", 3);
		boostDuration = getConfigInt("boost-duration", 300);
		
		hasted = new HashMap<Player,Integer>();
	}

	@Override
	public boolean castBuff(final Player player, float power, String[] args) {
		hasted.put(player, Math.round(strength*power));
		return true;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		if (hasted.containsKey(player)) {
			if (isExpired(player)) {
				turnOff(player);
			} else if (event.isSprinting()) {
				event.setCancelled(true);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, boostDuration, hasted.get(player)), true);
				addUseAndChargeCost(player);
				playSpellEffects(EffectPosition.CASTER, player);
			} else {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, 0), true);
				player.removePotionEffect(PotionEffectType.SPEED);
				playSpellEffects(EffectPosition.DISABLED, player);
			}
		}
	}

	@Override
	public void turnOffBuff(Player player) {
		if (hasted.remove(player) != null) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, 0), true);
			player.removePotionEffect(PotionEffectType.SPEED);
		}
	}
	
	@Override
	protected void turnOff() {
		for (Player p : hasted.keySet()) {
			p.removePotionEffect(PotionEffectType.SPEED);
		}
		hasted.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return hasted.containsKey(player);
	}

}
