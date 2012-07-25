package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ForcetossSpell extends TargetedEntitySpell {

	private int damage;
	private float hForce;
	private float vForce;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	
	public ForcetossSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigInt("damage", 0);
		hForce = getConfigInt("horizontal-force", 20) / 10.0F;
		vForce = getConfigInt("vertical-force", 10) / 10.0F;
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				return noTarget(player);
			}
			
			// do damage
			if (damage > 0) {
				int damage = Math.round(this.damage*power);
				if (target instanceof Player && checkPlugins) {
					EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, damage);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						return noTarget(player);
					}
					damage = event.getDamage();
				}
				target.damage(damage);
			}
			
			// throw target
			toss(player, target, power);
			
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void toss(Player player, LivingEntity target, float power) {
		Vector v = target.getLocation().toVector().subtract(player.getLocation().toVector())
				.setY(0)
				.normalize()
				.multiply(hForce*power)
				.setY(vForce*power);
		target.setVelocity(v);
		playSpellEffects(player, target);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player && !targetPlayers) {
			return false;
		} else {
			toss(caster, target, power);
			return true;
		}
	}
	
	

}
