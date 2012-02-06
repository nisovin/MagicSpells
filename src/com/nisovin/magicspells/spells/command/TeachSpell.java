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
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.MagicConfig;

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
		strUsage = config.getString("spells." + spellName + ".str-usage", "Usage: /cast teach <target> <spell>");
		strNoTarget = config.getString("spells." + spellName + ".str-no-target", "No such player.");
		strNoSpell = config.getString("spells." + spellName + ".str-no-spell", "You do not know a spell by that name.");
		strCantTeach = config.getString("spells." + spellName + ".str-cant-teach", "You can't teach that spell.");
		strCantLearn = config.getString("spells." + spellName + ".str-cant-learn", "That person cannot learn that spell.");
		strAlreadyKnown = config.getString("spells." + spellName + ".str-already-known", "That person already knows that spell.");
		strCastTarget = config.getString("spells." + spellName + ".str-cast-target", "%a has taught you the %s spell.");
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
							Spellbook targetSpellbook = MagicSpells.getSpellbook(players.get(0));
							if (targetSpellbook == null || !targetSpellbook.canLearn(spell)) {
								// fail: no spellbook for some reason or can't learn the spell
								sendMessage(player, strCantLearn);
							} else if (targetSpellbook.hasSpell(spell)) {
								// fail: target already knows spell
								sendMessage(player, strAlreadyKnown);
							} else {
								// call event
								boolean cancelled = callEvent(spell, players.get(0), player);
								if (cancelled) {
									// fail: plugin cancelled it
									sendMessage(player, strCantLearn);
								} else {									
									targetSpellbook.addSpell(spell);
									targetSpellbook.save();
									sendMessage(players.get(0), formatMessage(strCastTarget, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", players.get(0).getDisplayName()));
									sendMessage(player, formatMessage(strCastSelf, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", players.get(0).getDisplayName()));
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
							sendMessage(players.get(0), formatMessage(strCastTarget, "%a", MagicSpells.strConsoleName, "%s", spell.getName(), "%t", players.get(0).getDisplayName()));
							sender.sendMessage(formatMessage(strCastSelf, "%a", MagicSpells.strConsoleName, "%s", spell.getName(), "%t", players.get(0).getDisplayName()));
						}
					}
				}
			}
		}
		return true;
	}
	
	private boolean callEvent(Spell spell, Player learner, Object teacher) {
		SpellLearnEvent event = new SpellLearnEvent(spell, learner, LearnSource.TEACH, teacher);
		Bukkit.getServer().getPluginManager().callEvent(event);
		return event.isCancelled();
	}

}