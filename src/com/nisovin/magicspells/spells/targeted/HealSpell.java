package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class HealSpell extends TargetedEntitySpell {
	
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
				heal(player, target, power);
				
				sendMessage(player, formatMessage(strCastSelf, "%t", target.getDisplayName()));
				sendMessage(target, formatMessage(strCastTarget, "%a", player.getDisplayName()));
				sendMessageNear(player, formatMessage(strCastOthers, "%t", target.getDisplayName(), "%a", player.getDisplayName()));
				
				return PostCastAction.NO_MESSAGES;
			}
			
			return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void heal(Player player, Player target, float power) {			
		int health = target.getHealth();
		health += Math.round(healAmount*power);
		if (health > 20) health = 20;
		target.setHealth(health);
		
		if (showSpellEffect) {
			playPotionEffect(player, target, 0xFF0000, 40);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) {
			heal(caster, (Player)target, power);
			return true;
		} else {
			return false;
		}
	}

}
