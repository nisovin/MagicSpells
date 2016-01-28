package com.nisovin.magicspells.spells.instant;

import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class LeapSpell extends InstantSpell {
	
	private double forwardVelocity;
	private double upwardVelocity;
	private boolean cancelDamage;
	private boolean clientOnly;
	
	private HashSet<Player> jumping;
	
	public LeapSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		forwardVelocity = getConfigInt("forward-velocity", 40) / 10D;
		upwardVelocity = getConfigInt("upward-velocity", 15) / 10D;
		cancelDamage = getConfigBoolean("cancel-damage", true);
		clientOnly = getConfigBoolean("client-only", true);
		
		if (cancelDamage) {
			jumping = new HashSet<Player>();
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Vector v = player.getLocation().getDirection();
			v.setY(0).normalize().multiply(forwardVelocity*power).setY(upwardVelocity*power);
			if (clientOnly) {
				MagicSpells.getVolatileCodeHandler().setClientVelocity(player, v);
			} else {
				player.setVelocity(v);
			}
			if (cancelDamage) {
				jumping.add(player);
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		if (cancelDamage && event.getCause() == DamageCause.FALL && event.getEntity() instanceof Player && jumping.contains((Player)event.getEntity())) {
			event.setCancelled(true);
			jumping.remove((Player)event.getEntity());
			playSpellEffects(EffectPosition.TARGET, event.getEntity().getLocation());
		}
	}

}
