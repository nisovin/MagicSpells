package com.nisovin.magicspells.spells.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ForgetSpell extends CommandSpell {

	private boolean allowSelfForget;
	private String strUsage;
	private String strNoTarget;
	private String strNoSpell;
	private String strDoesntKnow;
	private String strCastTarget;
	private String strCastSelfTarget;
	private String strResetTarget;
	private String strResetSelf;
	
	public ForgetSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		allowSelfForget = getConfigBoolean("allow-self-forget", true);
		strUsage = getConfigString("str-usage", "Usage: /cast forget <target> <spell>");
		strNoTarget = getConfigString("str-no-target", "No such player.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strDoesntKnow = getConfigString("str-doesnt-know", "That person does not know that spell.");
		strCastTarget = getConfigString("str-cast-target", "%a has made you forget the %s spell.");
		strCastSelfTarget = getConfigString("str-cast-self-target", "You have forgotten the %s spell.");
		strResetTarget = getConfigString("str-reset-target", "You have reset %t's spellbook.");
		strResetSelf = getConfigString("str-reset-self", "You have forgotten all of your spells.");
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length == 0 || args.length > 2) {
				// fail: missing args
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			}
				// get caster spellbook
			Spellbook casterSpellbook = MagicSpells.getSpellbook(player);
			
			// get target
			Player target = null;
			if (args.length == 1 && allowSelfForget) {
				target = player;
			} else if (args.length == 2 && casterSpellbook.hasAdvancedPerm("forget")) {
				List<Player> players = MagicSpells.plugin.getServer().matchPlayer(args[0]);
				if (players.size() != 1) {
					// fail: no player match
					sendMessage(player, strNoTarget);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					target = players.get(0);
				}
			} else {
				// fail: missing args
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get spell
			String spellName = args.length == 1 ? args[0] : args[1];
			boolean all = false;
			Spell spell = null;
			if (spellName.equals("*")) {
				all = true;
			} else {
				spell = MagicSpells.getSpellByInGameName(spellName);
			}
			if (spell == null && !all) {
				// fail: no spell match
				sendMessage(player, strNoSpell);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// check if caster has spell
			if (!all && !casterSpellbook.hasSpell(spell)) {
				// fail: caster doesn't have spell
				sendMessage(player, strNoSpell);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get target spellbook and perform checks
			Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
			if (targetSpellbook == null || (!all && !targetSpellbook.hasSpell(spell))) {
				// fail: error
				sendMessage(player, strDoesntKnow);
				return PostCastAction.ALREADY_HANDLED;
			} 
			
			// remove spell(s)
			if (!all) {
				targetSpellbook.removeSpell(spell);
				targetSpellbook.save();
				if (!player.equals(target)) {
					sendMessage(target, formatMessage(strCastTarget, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()));
					sendMessage(player, formatMessage(strCastSelf, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()));
					playSpellEffects(EffectPosition.CASTER, player);
					playSpellEffects(EffectPosition.TARGET, target);
				} else {
					sendMessage(player, strCastSelfTarget, "%s", spell.getName());
					playSpellEffects(EffectPosition.CASTER, player);
				}
				return PostCastAction.NO_MESSAGES;
			} else if (all) {
				targetSpellbook.removeAllSpells();
				targetSpellbook.addGrantedSpells();
				targetSpellbook.save();
				if (!player.equals(target)) {
					sendMessage(player, strResetTarget, "%t", target.getDisplayName());
					playSpellEffects(EffectPosition.CASTER, player);
					playSpellEffects(EffectPosition.TARGET, target);
				} else {
					sendMessage(player, strResetSelf);
					playSpellEffects(EffectPosition.CASTER, player);
				}
				return PostCastAction.NO_MESSAGES;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length != 2) {
			// fail: missing args
			sender.sendMessage(strUsage);
		} else {
			Player target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				// fail: no player match
				sender.sendMessage(strNoTarget);
			} else {
				Spell spell = null;
				boolean all = false;
				if (args[1].equals("*")) {
					all = true;
				} else {
					spell = MagicSpells.getSpellByInGameName(args[1]);
				}
				if (spell == null && !all) {
					// fail: no spell match
					sender.sendMessage(strNoSpell);
				} else {
					Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
					if (targetSpellbook == null || (!all && !targetSpellbook.hasSpell(spell))) {
						// fail: no spellbook for some reason or can't learn the spell
						sender.sendMessage(strDoesntKnow);
					} else {
						SpellForgetEvent forgetEvent = new SpellForgetEvent(spell, target);
						Bukkit.getPluginManager().callEvent(forgetEvent);
						if (!forgetEvent.isCancelled()) {
							if (!all) {
								targetSpellbook.removeSpell(spell);
								targetSpellbook.save();
								sendMessage(target, formatMessage(strCastTarget, "%a", getConsoleName(), "%s", spell.getName(), "%t", target.getDisplayName()));
								sender.sendMessage(formatMessage(strCastSelf, "%a", getConsoleName(), "%s", spell.getName(), "%t", target.getDisplayName()));
							} else {
								targetSpellbook.removeAllSpells();
								targetSpellbook.addGrantedSpells();
								targetSpellbook.save();
								sender.sendMessage(formatMessage(strResetTarget, "%t", target.getDisplayName()));
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) {
			// matching player name or spell name
			List<String> options = new ArrayList<String>();
			List<String> players = tabCompletePlayerName(sender, args[0]);
			List<String> spells = tabCompleteSpellName(sender, args[0]);
			if (players != null) {
				options.addAll(players);
			}
			if (spells != null) {
				options.addAll(spells);
			}
			if (options.size() > 0) {
				return options;
			}
		} else if (args.length == 2) {
			// matching spell name
			return tabCompleteSpellName(sender, args[1]);
		}
		return null;
	}

}