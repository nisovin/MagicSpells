package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class HealSpell extends TargetedSpell {
	
	private int healAmount;
	private boolean cancelIfFull;
	private boolean showSpellEffect;
	private boolean obeyLos;
	private String strNoTarget;
	private String strMaxHealth;
	private String strCastTarget;

	public HealSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		healAmount = getConfigInt("heal-amount", 10);
		cancelIfFull = getConfigBoolean("cancel-if-full", true);
		showSpellEffect = getConfigBoolean("show-spell-effect", true);
		obeyLos = getConfigBoolean("obey-los", true);
		strNoTarget = getConfigString("str-no-target", "No target to heal.");
		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");
		strCastTarget = getConfigString("str-cast-target", "%a healed you.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Player target = getTargetedPlayer(player, range, obeyLos);
			if (target == null) {
				sendMessage(player, strNoTarget);
				fizzle(player);
			} else if (cancelIfFull && target.getHealth() == 20) {
				sendMessage(player, formatMessage(strMaxHealth, "%t", target.getName()));
			} else {				
				int health = target.getHealth();
				health += Math.round(healAmount*power);
				if (health > 20) health = 20;
				target.setHealth(health);
				
				if (showSpellEffect) {
					playPotionEffect(player, target, 0xFF0000, 40);
				}
				
				sendMessage(player, formatMessage(strCastSelf, "%t", target.getDisplayName()));
				sendMessage(target, formatMessage(strCastTarget, "%a", player.getDisplayName()));
				sendMessageNear(player, formatMessage(strCastOthers, "%t", target.getDisplayName(), "%a", player.getDisplayName()));
				
				return PostCastAction.NO_MESSAGES;
			}
			
			return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
