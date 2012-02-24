package com.nisovin.magicspells.spells.instant;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

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
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Random rand = new Random();
			Vector mod;
			for (int i = 0; i < snowballs; i++) {
				Snowball snowball = player.throwSnowball();
				snowball.setFallDistance(10.2F); // tag the snowballs
				mod = new Vector((rand.nextDouble() - .5) * horizSpread, (rand.nextDouble() - .5) * vertSpread, (rand.nextDouble() - .5) * horizSpread);
				snowball.setVelocity(snowball.getVelocity().add(mod));
			}
			if (playBowSound) {
				player.playEffect(player.getLocation(), Effect.BOW_FIRE, 0);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onEntityDamage(EntityDamageEvent event) {
		if (damage <= 0 || event.isCancelled() || !(event instanceof EntityDamageByEntityEvent)) return;
		
		EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
		if (!(evt.getDamager() instanceof Snowball) || evt.getDamager().getFallDistance() != 10.2F) return;
		
		if (targetPlayers || !(event.getEntity() instanceof Player)) {
			event.setDamage(damage);
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDamage2(EntityDamageEvent event) {
		if (slowAmount <= 0 || slowDuration <= 0) return;
		if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent)) return;
		
		EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
		if (!(evt.getDamager() instanceof Snowball) || evt.getDamager().getFallDistance() != 10.2F) return;
		
		if (event.getDamage() == damage) {
			((LivingEntity)event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowDuration, slowAmount), true);
		}
	}

}
