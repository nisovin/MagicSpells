package com.nisovin.magicspells.spells.buff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.server.EntityCreature;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftCreature;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class MinionSpell extends BuffSpell {
	
	private CreatureType[] creatureTypes;
	private int[] chances;
	private boolean preventCombust;
	private boolean targetPlayers;
	
	private HashMap<String,LivingEntity> minions;
	private HashMap<String,LivingEntity> targets;
	private Random random;
	
	public MinionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> c = config.getStringList("spells." + spellName + ".mob-chances", null);
		if (c == null) {
			c = new ArrayList<String>();
		}
		if (c.size() == 0) {
			c.add("Zombie 100");
		}
		creatureTypes = new CreatureType[c.size()];
		chances = new int[c.size()];
		for (int i = 0; i < c.size(); i++) {
			String[] data = c.get(i).split(" ");
			CreatureType creatureType = CreatureType.fromName(data[0]);
			int chance = 0;
			if (creatureType != null) {
				try {
					chance = Integer.parseInt(data[1]);
				} catch (NumberFormatException e) {
				}
			}
			creatureTypes[i] = creatureType;
			chances[i] = chance;
		}
		preventCombust = config.getBoolean("spells." + spellName + ".prevent-sun-burn", true);
		targetPlayers = config.getBoolean("spells." + spellName + ".target-players", false);
		
		minions = new HashMap<String,LivingEntity>();
		targets = new HashMap<String,LivingEntity>();
		random = new Random();
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (minions.containsKey(player.getName())) {
			LivingEntity minion = minions.get(player.getName());
			if (!minion.isDead()) { // don't toggle off if the minion is dead
				turnOff(player);
				return PostCastAction.ALREADY_HANDLED;
			}
		} 
		if (state == SpellCastState.NORMAL) {
			CreatureType creatureType = null;
			int num = random.nextInt(100);
			int n = 0;
			for (int i = 0; i < creatureTypes.length; i++) {
				if (num < chances[i] + n) {
					creatureType = creatureTypes[i];
					break;
				} else {
					n += chances[i];
				}
			}
			if (creatureType != null) {
				// get spawn location TODO: improve this
				Location loc = null;
				loc = player.getLocation();
				loc.setX(loc.getX()-1);
				
				// spawn creature
				LivingEntity minion = player.getWorld().spawnCreature(loc, creatureType);
				minions.put(player.getName(), minion);
				targets.put(player.getName(), null);
				startSpellDuration(player);
			} else {
				// fail -- no creature found
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if (!event.isCancelled() && minions.size() > 0 ) {	
			if (event.getTarget() instanceof Player) {
				// a monster is trying to target a player
				Player player = (Player)event.getTarget();
				LivingEntity minion = minions.get(player.getName());
				if (minion != null && minion.getEntityId() == event.getEntity().getEntityId()) {
					// the targeted player owns the minion
					if (isExpired(player)) {
						// spell is expired
						turnOff(player);
						return;
					}
					// check if the player has a current target
					LivingEntity target = targets.get(player.getName());
					if (target != null) {
						// player has a target
						if (target.isDead()) {
							// the target is dead, so remove that target
							targets.put(player.getName(), null);
							event.setCancelled(true);
						} else {
							// send the minion after the player's target
							event.setTarget(targets.get(player.getName()));
							addUse(player);
							chargeUseCost(player);
						}
					} else {
						// player doesn't have a target, so just order the minion to follow
						event.setCancelled(true);
						double distSq = minion.getLocation().toVector().distanceSquared(player.getLocation().toVector());
						if (distSq > 3*3) {
							// minion is too far, tell him to move closer
							EntityCreature entity = ((CraftCreature)minion).getHandle();
							entity.pathEntity = entity.world.findPath(entity, ((CraftPlayer)player).getHandle(), 16.0F);
						} 
					}
				} else if (!targetPlayers && minions.containsValue(event.getEntity())) {
					// player doesn't own minion, but it is an owned minion and pvp is off, so cancel
					event.setCancelled(true);
				}				
			} else if (event.getReason() == TargetReason.FORGOT_TARGET && minions.containsValue(event.getEntity())) {
				// forgetting target but it's a minion, don't let them do that! (probably a spider going passive)
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (minions.containsValue(event.getEntity())) {
			event.setDroppedExp(0);
		}
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (!event.isCancelled() && event instanceof EntityDamageByEntityEvent && event.getEntity() instanceof LivingEntity) {
			EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
			Player p = null;
			if (evt.getDamager() instanceof Player) {
				p = (Player)evt.getDamager();
			} else if (evt.getDamager() instanceof Projectile && ((Projectile)evt.getDamager()).getShooter() instanceof Player) {
				p = (Player)((Projectile)evt.getDamager()).getShooter();
			}
			if (p != null) {
				if (minions.containsKey(p.getName())) {
					if (isExpired(p)) {
						turnOff(p);
						return;
					}
					LivingEntity target = (LivingEntity)event.getEntity();
					((Creature)minions.get(p.getName())).setTarget(target);
					targets.put(p.getName(), target);
					addUse(p);
					chargeUseCost(p);
				}
			}
		}
	}	

	@EventHandler
	public void onEntityCombust(EntityCombustEvent event) {
		if (preventCombust && !event.isCancelled() && minions.containsValue(event.getEntity())) {
			event.setCancelled(true);
		}
	}
	
	@Override
	public void turnOff(Player player) {
		super.turnOff(player);
		LivingEntity minion = minions.get(player.getName());
		if (minion != null && !minion.isDead()) {
			minion.setHealth(0);
			sendMessage(player, strFade);
		}
		minions.remove(player.getName());
		targets.remove(player.getName());
	}
	
	@Override
	protected void turnOff() {
		for (LivingEntity minion : minions.values()) {
			minion.setHealth(0);
		}
		minions.clear();
		targets.clear();
	}
	
}