package com.nisovin.magicspells.spells.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class TeachSpell extends CommandSpell {

	private boolean requireKnownSpell;
	private String strUsage;
	private String strNoTarget;
	private String strNoSpell;
	private String strCantTeach;
	private String strCantLearn;
	private String strAlreadyKnown;
	private String strCastTarget;
	
	public TeachSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireKnownSpell = getConfigBoolean("require-known-spell", true);
		strUsage = getConfigString("str-usage", "Usage: /cast teach <target> <spell>");
		strNoTarget = getConfigString("str-no-target", "No such player.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strCantTeach = getConfigString("str-cant-teach", "You can't teach that spell.");
		strCantLearn = getConfigString("str-cant-learn", "That person cannot learn that spell.");
		strAlreadyKnown = getConfigString("str-already-known", "That person already knows that spell.");
		strCastTarget = getConfigString("str-cast-target", "%a has taught you the %s spell.");
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length != 2) {
				// fail: missing args
				sendMessage(player, strUsage);
			} else {
				List<Player> players = MagicSpells.plugin.getServer().matchPlayer(args[0]);
				if (players.size() != 1) {
					// fail: no player match
					sendMessage(player, strNoTarget);
				} else {
					Spell spell = MagicSpells.getSpellByInGameName(args[1]);
					Player target = players.get(0);
					if (spell == null) {
						// fail: no spell match
						sendMessage(player, strNoSpell);
					} else {
						Spellbook spellbook = MagicSpells.getSpellbook(player);
						if (spellbook == null || (!spellbook.hasSpell(spell) && requireKnownSpell)) {
							// fail: player doesn't have spell
							sendMessage(player, strNoSpell);
						} else if (!spellbook.canTeach(spell)) {
							// fail: cannot teach
							sendMessage(player, strCantTeach);
						} else {
							// yay! can learn!
							Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
							if (targetSpellbook == null || !targetSpellbook.canLearn(spell)) {
								// fail: no spellbook for some reason or can't learn the spell
								sendMessage(player, strCantLearn);
							} else if (targetSpellbook.hasSpell(spell)) {
								// fail: target already knows spell
								sendMessage(player, strAlreadyKnown);
							} else {
								// call event
								boolean cancelled = callEvent(spell, target, player);
								if (cancelled) {
									// fail: plugin cancelled it
									sendMessage(player, strCantLearn);
								} else {									
									targetSpellbook.addSpell(spell);
									targetSpellbook.save();
									sendMessage(target, formatMessage(strCastTarget, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()));
									sendMessage(player, formatMessage(strCastSelf, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()));
									playSpellEffects(EffectPosition.CASTER, player);
									playSpellEffects(EffectPosition.TARGET, target);
									return PostCastAction.NO_MESSAGES;
								}
							}
						}
					}
				}
			}
			return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length != 2) {
			// fail: missing args
			sender.sendMessage(strUsage);
		} else {
			List<Player> players = MagicSpells.plugin.getServer().matchPlayer(args[0]);
			if (players.size() != 1) {
				// fail: no player match
				sender.sendMessage(strNoTarget);
			} else {
				Spell spell = MagicSpells.getSpellByInGameName(args[1]);
				if (spell == null) {
					// fail: no spell match
					sender.sendMessage(strNoSpell);
				} else {
					// yay! can learn!
					Spellbook targetSpellbook = MagicSpells.getSpellbook(players.get(0));
					if (targetSpellbook == null || !targetSpellbook.canLearn(spell)) {
						// fail: no spellbook for some reason or can't learn the spell
						sender.sendMessage(strCantLearn);
					} else if (targetSpellbook.hasSpell(spell)) {
						// fail: target already knows spell
						sender.sendMessage(strAlreadyKnown);
					} else {
						// call event
						boolean cancelled = callEvent(spell, players.get(0), sender);
						if (cancelled) {
							// fail: cancelled by plugin
							sender.sendMessage(strCantLearn);
						} else {
							targetSpellbook.addSpell(spell);
							targetSpellbook.save();
							sendMessage(players.get(0), formatMessage(strCastTarget, "%a", getConsoleName(), "%s", spell.getName(), "%t", players.get(0).getDisplayName()));
							sender.sendMessage(formatMessage(strCastSelf, "%a", getConsoleName(), "%s", spell.getName(), "%t", players.get(0).getDisplayName()));
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
			// matching player name
			return tabCompletePlayerName(sender, args[0]);
		} else if (args.length == 2) {
			// matching spell name
			return tabCompleteSpellName(sender, args[1]);
		}
		return null;
	}
	
	private boolean callEvent(Spell spell, Player learner, Object teacher) {
		SpellLearnEvent event = new SpellLearnEvent(spell, learner, LearnSource.TEACH, teacher);
		Bukkit.getServer().getPluginManager().callEvent(event);
		return event.isCancelled();
	}

}