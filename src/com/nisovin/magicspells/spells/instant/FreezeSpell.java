package com.nisovin.magicspells.spells.instant;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class FreezeSpell extends InstantSpell {

	private int snowballs;
	private double horizSpread;
	private double vertSpread;
	private int damage;
	private int slowAmount;
	private int slowDuration;
	private boolean playBowSound;
	private boolean targetPlayers;
	private boolean callTargetEvents;
	
	private float identifier;
	
	public FreezeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		snowballs = getConfigInt("snowballs", 15);
		horizSpread = getConfigInt("horizontal-spread", 15) / 10.0;
		vertSpread = getConfigInt("vertical-spread", 15) / 10.0;
		damage = getConfigInt("damage", 3);
		slowAmount = getConfigInt("slow-amount", 3);
		slowDuration = getConfigInt("slow-duration", 40);
		playBowSound = getConfigBoolean("play-bow-sound", true);
		targetPlayers = getConfigBoolean("target-players", false);
		callTargetEvents = getConfigBoolean("call-target-events", false);
		
		identifier = (float)Math.random() * 20F;
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Random rand = new Random();
			Vector mod;
			for (int i = 0; i < snowballs; i++) {
				Snowball snowball = player.launchProjectile(Snowball.class);
				snowball.setFallDistance(identifier); // tag the snowballs
				mod = new Vector((rand.nextDouble() - .5) * horizSpread, (rand.nextDouble() - .5) * vertSpread, (rand.nextDouble() - .5) * horizSpread);
				snowball.setVelocity(snowball.getVelocity().add(mod));
			}
			if (playBowSound) {
				player.playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if (damage <= 0 || event.isCancelled()) return;
		
		if (!(event.getDamager() instanceof Snowball) || event.getDamager().getFallDistance() != identifier) return;
		
		if (targetPlayers || !(event.getEntity() instanceof Player)) {
			if (callTargetEvents) {
				SpellTargetEvent e = new SpellTargetEvent(this, (Player)((Snowball)event.getDamager()).getShooter(), (LivingEntity)event.getEntity());
				Bukkit.getPluginManager().callEvent(e);
				if (e.isCancelled()) {
					event.setCancelled(true);
				} else {
					event.setDamage(damage);
				}
			} else {				
				event.setDamage(damage);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onEntityDamage2(EntityDamageByEntityEvent event) {
		if (slowAmount <= 0 || slowDuration <= 0) return;
		
		if (!(event.getDamager() instanceof Snowball) || event.getDamager().getFallDistance() != identifier) return;
		
		if (event.getDamage() == damage) {
			((LivingEntity)event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmount), true);
		}
	}

}
