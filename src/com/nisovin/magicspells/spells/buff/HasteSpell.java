package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.MagicSpells;
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
	public PostCastAction castSpell(final Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			hasted.put(player, Math.round(strength*power));
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		if (hasted.containsKey(player)) {
			if (isExpired(player)) {
				playGraphicalEffects(2, player);
				turnOff(player);
			} else if (event.isSprinting()) {
				event.setCancelled(true);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, boostDuration, hasted.get(player)), true);
				addUseAndChargeCost(player);
				playGraphicalEffects(1, player);
			} else {
				MagicSpells.getVolatileCodeHandler().removeMobEffect(player, PotionEffectType.SPEED);
				playGraphicalEffects(2, player);
			}
		}
	}

	@Override
	public void turnOff(Player player) {
		if (hasted.containsKey(player)) {
			super.turnOff(player);
			hasted.remove(player);
			MagicSpells.getVolatileCodeHandler().removeMobEffect(player, PotionEffectType.SPEED);
			sendMessage(player, strFade);
		}
	}
	
	@Override
	protected void turnOff() {
		for (Player p : hasted.keySet()) {
			p.removePotionEffect(PotionEffectType.SPEED);
		}
		hasted.clear();
	}

}
