package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.util.SpellReagents;

public class PortalSpell extends InstantSpell {

	private String markSpellName;
	private int duration;
	private int teleportCooldown;
	private int minDistanceSq;
	private int maxDistanceSq;
	private int effectInterval;
	private SpellReagents teleportCost;
	private boolean allowReturn;
	private boolean chargeCostToTeleporter;
	
	private String strNoMark;
	private String strTooClose;
	private String strTooFar;
	private String strTeleportCostFail;
	private String strTeleportCooldownFail;
	
	private HashMap<String, MagicLocation> marks;
	
	public PortalSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		markSpellName = getConfigString("mark-spell", "mark");
		duration = getConfigInt("duration", 400);
		teleportCooldown = getConfigInt("teleport-cooldown", 5) * 1000;
		minDistanceSq = getConfigInt("min-distance", 10);
		minDistanceSq *= minDistanceSq;
		maxDistanceSq = getConfigInt("max-distance", 0);
		maxDistanceSq *= maxDistanceSq;
		effectInterval = getConfigInt("effect-interval", 10);
		teleportCost = getConfigReagents("teleport-cost");
		allowReturn = getConfigBoolean("allow-return", true);
		chargeCostToTeleporter = getConfigBoolean("charge-cost-to-teleporter", false);
		
		strNoMark = getConfigString("str-no-mark", "You have not marked a location to make a portal to.");
		strTooClose = getConfigString("str-too-close", "You are too close to your marked location.");
		strTooFar = getConfigString("str-too-far", "You are too far away from your marked location.");
		strTeleportCostFail = getConfigString("str-teleport-cost-fail", "");
		strTeleportCooldownFail = getConfigString("str-teleport-cooldown-fail", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell != null && spell instanceof MarkSpell) {
			marks = ((MarkSpell)spell).getMarks();
		} else {
			MagicSpells.error("Failed to get marks list for '" + internalName + "' spell");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			MagicLocation mark = marks.get(player.getName().toLowerCase());
			if (mark != null) {
				loc = mark.getLocation();
			}
			if (loc == null) {
				// no mark
				sendMessage(player, strNoMark);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				
				Location playerLoc = player.getLocation();
				
				double distanceSq = 0;
				if (maxDistanceSq > 0) {
					if (!loc.getWorld().equals(playerLoc.getWorld())) {
						sendMessage(player, strTooFar);
						return PostCastAction.ALREADY_HANDLED;
					} else {
						distanceSq = playerLoc.distanceSquared(loc);
						if (distanceSq > maxDistanceSq) {
							sendMessage(player, strTooFar);
							return PostCastAction.ALREADY_HANDLED;
						}
					}
				}
				if (minDistanceSq > 0) {
					if (loc.getWorld().equals(playerLoc.getWorld())) {
						if (distanceSq == 0) distanceSq = playerLoc.distanceSquared(loc);
						if (distanceSq < minDistanceSq) {
							sendMessage(player, strTooClose);
							return PostCastAction.ALREADY_HANDLED;
						}
					}
				}
				
				new PortalLink(this, player, playerLoc, loc);
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean isBeneficialDefault() {
		return true;
	}
	
	class PortalLink implements Listener {
		
		PortalSpell spell;
		Player caster;
		Location loc1;
		Location loc2;
		BoundingBox box1;
		BoundingBox box2;
		int taskId1 = -1;
		int taskId2 = -1;
		Map<String, Long> cooldownUntil = new HashMap<String, Long>();
		
		public PortalLink (PortalSpell spell, Player caster, Location loc1, Location loc2) {
			this.spell = spell;
			this.caster = caster;
			this.loc1 = loc1;
			this.loc2 = loc2;
			this.box1 = new BoundingBox(loc1, .6);
			this.box2 = new BoundingBox(loc2, .6);
			
			cooldownUntil.put(caster.getName(), System.currentTimeMillis() + teleportCooldown);
			registerEvents(this);
			startTasks();
		}
		
		void startTasks() {
			if (effectInterval > 0) {
				taskId1 = MagicSpells.scheduleRepeatingTask(new Runnable() {
					public void run() {
						if (caster.isValid()) {
							playSpellEffects(EffectPosition.SPECIAL, loc1);
							playSpellEffects(EffectPosition.SPECIAL, loc2);
						} else {
							disable();
						}
					}
				}, effectInterval, effectInterval);
			}
			taskId2 = MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					disable();
				}
			}, duration);
		}
		
		@EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
		void onMove(PlayerMoveEvent event) {
			if (caster.isValid()) {
				Player player = event.getPlayer();
				if (box1.contains(event.getTo())) {
					if (checkTeleport(player)) {
						Location loc = loc2.clone();
						loc.setYaw(player.getLocation().getYaw());
						loc.setPitch(player.getLocation().getPitch());
						event.setTo(loc);
						playSpellEffects(EffectPosition.TARGET, player);
					}
				} else if (allowReturn && box2.contains(event.getTo())) {
					if (checkTeleport(player)) {
						Location loc = loc1.clone();
						loc.setYaw(player.getLocation().getYaw());
						loc.setPitch(player.getLocation().getPitch());
						event.setTo(loc);
						playSpellEffects(EffectPosition.TARGET, player);
					}
				}
			} else {
				disable();
			}
		}
		
		boolean checkTeleport(Player player) {
			if (cooldownUntil.containsKey(player.getName()) && cooldownUntil.get(player.getName()) > System.currentTimeMillis()) {
				sendMessage(player, strTeleportCooldownFail);
				return false;
			}
			cooldownUntil.put(player.getName(), System.currentTimeMillis() + teleportCooldown);
			
			Player payer = null;
			if (teleportCost != null) {
				if (chargeCostToTeleporter) {
					if (hasReagents(player, teleportCost)) {
						payer = player;
					} else {
						sendMessage(player, strTeleportCostFail);
						return false;
					}
				} else {
					if (hasReagents(caster, teleportCost)) {
						payer = caster;
					} else {
						sendMessage(player, strTeleportCostFail);
						return false;
					}
				}
				if (payer == null) return false;
			}
			
			SpellTargetEvent event = new SpellTargetEvent(spell, caster, player, 1);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			}
			
			if (payer != null) {
				removeReagents(payer, teleportCost);
			}
			return true;
		}
		
		void disable() {
			playSpellEffects(EffectPosition.DELAYED, loc1);
			playSpellEffects(EffectPosition.DELAYED, loc2);
			unregisterEvents(this);
			if (taskId1 > 0) {
				MagicSpells.cancelTask(taskId1);
			}
			if (taskId2 > 0) {
				MagicSpells.cancelTask(taskId2);
			}
			spell = null;
			caster = null;
			loc1 = null;
			loc2 = null;
			box1 = null;
			box2 = null;
			cooldownUntil.clear();
			cooldownUntil = null;
		}
		
	}

}
