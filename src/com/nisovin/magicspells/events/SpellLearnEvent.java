package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.magicspells.Spell;

/**
 * This event is fired whenever a player is about to learn a spell, either from
 * the teach spell, a spellbook, a tome, or from an external plugin calling the
 * MagicSpells.teachSpell method.
 *
 */
public class SpellLearnEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

	private Spell spell;
	private Player learner;
	private LearnSource source;
	private Object teacher;
	private boolean cancelled;
	
	public SpellLearnEvent(Spell spell, Player learner, LearnSource source, Object teacher) {
		this.spell = spell;
		this.learner = learner;
		this.source = source;
		this.teacher = teacher;
		this.cancelled = false;
	}

	/**
	 * Gets the spell that is going to be learned
	 * @return the learned spell
	 */
	public Spell getSpell() {
		return spell;
	}
	
	/**
	 * Gets the player that will be learning the spell
	 * @return the learning player
	 */
	public Player getLearner() {
		return learner;
	}
	
	/**
	 * Gets the source of the learning (teach, spellbook, tome, other)
	 * @return the source
	 */
	public LearnSource getSource() {
		return source;
	}
	
	/**
	 * Gets the object that is teaching the spell
	 * @return the player/console for teach, the block for spellbook, or the book item for tome, or null
	 */
	public Object getTeacher() {
		return teacher;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
	
	public enum LearnSource {
		TEACH,
		SPELLBOOK,
		TOME,
		MAGIC_XP,
		OTHER
	}

}
