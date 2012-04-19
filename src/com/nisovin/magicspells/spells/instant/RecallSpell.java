package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;

public class RecallSpell extends InstantSpell {
	
	private String markSpellName;
	private boolean allowCrossWorld;
	private int maxRange;
	private int castTime;
	private boolean useBedLocation;
	private String strNoMark;
	private String strOtherWorld;
	private String strTooFar;
	private String strCastDone;
	private String strCastInterrupted;
	private String strRecallFailed;
	
	private HashMap<String,MagicLocation> marks;
	private HashSet<String> casting;

	public RecallSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		markSpellName = getConfigString("mark-spell", "mark");
		allowCrossWorld = getConfigBoolean("allow-cross-world", true);
		maxRange = getConfigInt("max-range", 0);
		castTime = getConfigInt("cast-time", 0);
		useBedLocation = getConfigBoolean("use-bed-location", false);
		strNoMark = getConfigString("str-no-mark", "You have no mark to recall to.");
		strOtherWorld = getConfigString("str-other-world", "Your mark is in another world.");
		strTooFar = getConfigString("str-too-far", "You mark is too far away.");
		strCastDone = getConfigString("str-cast-done", "");
		strCastInterrupted = getConfigString("str-cast-interrupted", "");
		strRecallFailed = getConfigString("str-recall-failed", "Could not recall.");
		
		if (castTime > 0) {
			casting = new HashSet<String>();
		}
	}
	
	@Override
	public void initialize() {
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
			Location mark = null;
			if (useBedLocation) {
				mark = player.getBedSpawnLocation();
			} else if (marks != null) {
				MagicLocation loc = marks.get(player.getName());
				if (loc != null) {
					mark = loc.getLocation();
				}
			}
			if (mark == null) {
				sendMessage(player, strNoMark);
				return PostCastAction.ALREADY_HANDLED;
			} else if (!allowCrossWorld && !mark.getWorld().getName().equals(player.getLocation().getWorld().getName())) {
				// can't cross worlds
				sendMessage(player, strOtherWorld);
				return PostCastAction.ALREADY_HANDLED;
			} else if (maxRange > 0 && mark.toVector().distanceSquared(player.getLocation().toVector()) > maxRange*maxRange) {
				// too far
				sendMessage(player, strTooFar);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				// all good!
				if (castTime > 0) {
					// wait a bit
					casting.add(player.getName());
					Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Teleporter(player, mark), castTime);
				} else {
					// go instantly
					Location from = player.getLocation();
					boolean teleported = player.teleport(mark);
					if (teleported) {
						playGraphicalEffects(1, from);
						playGraphicalEffects(2, mark);
					} else {
						// fail -- teleport prevented
						MagicSpells.error("Recall teleport blocked for " + player.getName());
						sendMessage(player, strRecallFailed);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		if (castTime <= 0) return;
		
		Entity e = event.getEntity();
		if (e instanceof Player) {
			String name = ((Player)e).getName();
			if (casting.contains(name)) {
				casting.remove(name);
				sendMessage((Player)e, strCastInterrupted);
			}
		}
	}
	
	private class Teleporter implements Runnable {
		private Player player;
		private Location location;
		private Location mark;
		
		public Teleporter(Player player, Location mark) {
			this.player = player;
			this.location = player.getLocation().clone();
			this.mark = mark;
		}
		
		public void run() {
			if (casting.contains(player.getName())) {
				casting.remove(player.getName());
				Location loc = player.getLocation();
				if (Math.abs(location.getX()-loc.getX()) < .1 && Math.abs(location.getY()-loc.getY()) < .1 && Math.abs(location.getZ()-loc.getZ()) < .1) {
					boolean teleported = player.teleport(mark);
					if (teleported) {
						playGraphicalEffects(1, location);
						playGraphicalEffects(2, mark);
						sendMessage(player, strCastDone);
					} else {
						// fail -- teleport prevented
						MagicSpells.error("Recall teleport blocked for " + player.getName());
						sendMessage(player, strRecallFailed);
					}
				} else {
					sendMessage(player, strCastInterrupted);
				}
			}
		}
	}

}
