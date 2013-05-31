package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class HealSpell extends TargetedEntitySpell {
	
	private int healAmount;
	private boolean cancelIfFull;
	private boolean obeyLos;
	private boolean checkPlugins;
	private String strMaxHealth;

	public HealSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		healAmount = getConfigInt("heal-amount", 10);
		cancelIfFull = getConfigBoolean("cancel-if-full", true);
		obeyLos = getConfigBoolean("obey-los", true);
		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");
		checkPlugins = getConfigBoolean("check-plugins", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Player target = (Player)getTargetedEntity(player, minRange, range, true, false, obeyLos, true, new ValidTargetChecker() {				
				@Override
				public boolean isValidTarget(LivingEntity entity) {
					return entity instanceof Player && entity.getHealth() < entity.getMaxHealth();
				}
			});
			//Player target = getTargetedPlayer(player, minRange, range, obeyLos);
			if (target == null) {
				return noTarget(player);
			} else if (cancelIfFull && target.getHealth() == target.getMaxHealth()) {
				return noTarget(player, formatMessage(strMaxHealth, "%t", target.getName()));
			} else {
				boolean healed = heal(player, target, power);
				if (!healed) {
					return noTarget(player);
				}
				sendMessages(player, target);				
				return PostCastAction.NO_MESSAGES;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean heal(Player player, Player target, float power) {
		int health = target.getHealth();
		int amt = Math.round(healAmount*power);
		if (checkPlugins) {
			EntityRegainHealthEvent evt = new EntityRegainHealthEvent(target, amt, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(evt);
			if (evt.isCancelled()) {
				return false;
			}
			amt = evt.getAmount();
		}
		health += amt;
		if (health > target.getMaxHealth()) health = target.getMaxHealth();
		target.setHealth(health);
		
		playSpellEffects(EffectPosition.CASTER, player);
		playSpellEffects(EffectPosition.TARGET, target, "FF0000 40");
		playSpellEffectsTrail(player.getLocation(), target.getLocation(), null);
		
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) {
			return heal(caster, (Player)target, power);
		} else {
			return false;
		}
	}
	
	@Override
	public boolean isBeneficial() {
		return true;
	}

}
