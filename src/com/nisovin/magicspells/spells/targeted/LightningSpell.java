package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class LightningSpell extends TargetedLocationSpell {
	
	private boolean requireEntityTarget;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	private int additionalDamage;
	private boolean noDamage;
	private String strCastFail;
	
	public LightningSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		additionalDamage = getConfigInt("additional-damage", 0);
		noDamage = getConfigBoolean("no-damage", false);		
		strCastFail = getConfigString("str-cast-fail", "");
	}
	
	@Override
	public void initialize() {
		if (!targetPlayers) {
			registerEvents(new PlayerDamageListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = null;
			LivingEntity entityTarget = null;
			if (requireEntityTarget) {
				entityTarget = getTargetedEntity(player, range, targetPlayers, obeyLos);
				if (entityTarget != null && entityTarget instanceof Player && checkPlugins) {
					EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, entityTarget, DamageCause.ENTITY_ATTACK, 0);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						entityTarget = null;
					}					
				}
				if (entityTarget != null) {
					target = entityTarget.getLocation().getBlock();
					if (additionalDamage > 0) {
						entityTarget.damage(Math.round(additionalDamage*power), player);
					}
				} else {
					sendMessage(player, strNoTarget);
					fizzle(player);
					return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
				}
			} else {
				try {
					target = player.getTargetBlock(MagicSpells.getTransparentBlocks(), range);
				} catch (IllegalStateException e) {	
					target = null;
				}
			}
			if (target != null) {
				lightning(target.getLocation());
				playGraphicalEffects(player, target.getLocation());
				if (entityTarget != null) {
					sendMessages(player, entityTarget);
					return PostCastAction.NO_MESSAGES;
				}
			} else {
				sendMessage(player, strCastFail);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void lightning(Location target) {
		if (noDamage) {
			target.getWorld().strikeLightningEffect(target);
		} else {				
			target.getWorld().strikeLightning(target);
		}
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		lightning(target);
		playGraphicalEffects(caster, target);
		return true;
	}
	
	public class PlayerDamageListener implements Listener {
		@EventHandler(ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (!targetPlayers && event.getCause() == DamageCause.LIGHTNING && event.getEntity() instanceof Player) {
				event.setCancelled(true);
			}
		}
	}
}
