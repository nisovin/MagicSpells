package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;

public class RecallSpell extends InstantSpell implements TargetedEntitySpell {
	
	private String markSpellName;
	private boolean allowCrossWorld;
	private int maxRange;
	private boolean useBedLocation;
	private String strNoMark;
	private String strOtherWorld;
	private String strTooFar;
	private String strRecallFailed;
	
	private HashMap<String,MagicLocation> marks;

	public RecallSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		markSpellName = getConfigString("mark-spell", "mark");
		allowCrossWorld = getConfigBoolean("allow-cross-world", true);
		maxRange = getConfigInt("max-range", 0);
		useBedLocation = getConfigBoolean("use-bed-location", false);
		strNoMark = getConfigString("str-no-mark", "You have no mark to recall to.");
		strOtherWorld = getConfigString("str-other-world", "Your mark is in another world.");
		strTooFar = getConfigString("str-too-far", "You mark is too far away.");
		strRecallFailed = getConfigString("str-recall-failed", "Could not recall.");
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
			Location mark = null;
			if (args != null && args.length == 1 && player.hasPermission("magicspells.advanced." + internalName)) {
				Player target = Bukkit.getPlayer(args[0]);				
				if (useBedLocation) {
					if (target != null) {
						mark = target.getBedSpawnLocation();
					}
				} else if (marks != null) {
					MagicLocation loc = marks.get(target != null ? target.getName().toLowerCase() : args[0].toLowerCase());
					if (loc != null) {
						mark = loc.getLocation();
					}
				}
			} else {
				mark = getRecallLocation(player);
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
				Location from = player.getLocation();
				boolean teleported = player.teleport(mark);
				if (teleported) {
					playSpellEffects(EffectPosition.CASTER, from);
					playSpellEffects(EffectPosition.TARGET, mark);
				} else {
					// fail -- teleport prevented
					MagicSpells.error("Recall teleport blocked for " + player.getName());
					sendMessage(player, strRecallFailed);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	Location getRecallLocation(Player caster) {
		if (useBedLocation) {
			return caster.getBedSpawnLocation();
		} else if (marks != null) {
			MagicLocation loc = marks.get(caster.getName().toLowerCase());
			if (loc != null) {
				return loc.getLocation();
			}
		}
		return null;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		Location mark = getRecallLocation(caster);
		if (mark != null) {
			target.teleport(mark);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
