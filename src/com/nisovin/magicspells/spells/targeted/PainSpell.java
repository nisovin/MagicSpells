package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PainSpell extends TargetedSpell {

	private int damage;
	private boolean ignoreArmor;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	private String strNoTarget;
	
	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigInt("damage", 4);
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		strNoTarget = getConfigString("str-no-target", "No target found.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail -- no target
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				int dam = Math.round(damage*power);
				if (target instanceof Player && checkPlugins) {
					// handle the event myself so I can detect cancellation properly
					EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, dam);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						sendMessage(player, strNoTarget);
						fizzle(player);
						return PostCastAction.ALREADY_HANDLED;
					}
					dam = event.getDamage();
				}
				if (ignoreArmor) {
					int health = target.getHealth() - dam;
					if (health < 0) health = 0;
					target.setHealth(health);
				} else {
					target.damage(dam);
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
