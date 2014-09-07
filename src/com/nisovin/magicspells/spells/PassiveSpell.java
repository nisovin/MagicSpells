package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.passive.PassiveManager;
import com.nisovin.magicspells.spells.passive.PassiveTrigger;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class PassiveSpell extends Spell {

	private static PassiveManager manager;
	
	private Random random = new Random();
	private boolean disabled = false;
	
	private List<String> triggers;
	private float chance;
	private boolean castWithoutTarget;
	private int delay;
	private boolean cancelDefaultAction;
	private boolean ignoreCancelled;
	private boolean sendFailureMessages;
	
	private List<String> spellNames;
	private List<Subspell> spells;
	
	public PassiveSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		if (manager == null) manager = new PassiveManager();
		
		triggers = getConfigStringList("triggers", null);
		chance = getConfigFloat("chance", 100F) / 100F;
		castWithoutTarget = getConfigBoolean("cast-without-target", false);
		delay = getConfigInt("delay", -1);
		cancelDefaultAction = getConfigBoolean("cancel-default-action", false);
		ignoreCancelled = getConfigBoolean("ignore-cancelled", true);
		sendFailureMessages = getConfigBoolean("send-failure-messages", false);
		
		spellNames = getConfigStringList("spells", null);
	}
	
	public static PassiveManager getManager() {
		return manager;
	}
	
	public List<Subspell> getActivatedSpells() {
		return spells;
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		// create spell list
		spells = new ArrayList<Subspell>();
		if (spellNames != null) {
			for (String spellName : spellNames) {
				Subspell spell = new Subspell(spellName);
				if (spell.process()) {
					spells.add(spell);
				}
			}
		}
		if (spells.size() == 0) {
			MagicSpells.error("Passive spell '" + name + "' has no spells defined!");
			return;
		}
		
		// get trigger
		int trigCount = 0;
		if (triggers != null) {
			for (String strigger : triggers) {
				String type = strigger;
				String var = null;
				if (strigger.contains(" ")) {
					String[] data = Util.splitParams(strigger, 2);
					type = data[0];
					var = data[1];
				}
				type = type.toLowerCase();
				
				PassiveTrigger trigger = PassiveTrigger.getByName(type);
				if (trigger != null) {
					manager.registerSpell(this, trigger, var);
					trigCount++;
				} else {
					MagicSpells.error("Invalid trigger '" + strigger + "' on passive spell '" + internalName + "'");
				}
			}
		}
		if (trigCount == 0) {
			MagicSpells.error("Passive spell '" + name + "' has no triggers defined!");
			return;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}
	
	public boolean activate(Player caster) {
		return activate(caster, null, null);
	}
	
	public boolean activate(Player caster, float power) {
		return activate(caster, null, null, power);
	}
	
	public boolean activate(Player caster, LivingEntity target) {
		return activate(caster, target, null, 1F);
	}
	
	public boolean activate(Player caster, Location location) {
		return activate(caster, null, location, 1F);
	}
	
	public boolean activate(final Player caster, final LivingEntity target, final Location location) {
		return activate(caster, target, location, 1F);
	}
	
	public boolean activate(final Player caster, final LivingEntity target, final Location location, final float power) {
		if (delay < 0) {
			return activateSpells(caster, target, location, power);
		} else {
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					activateSpells(caster, target, location, power);
				}
			}, delay);
			return false;
		}
	}
	
	private boolean activateSpells(Player caster, LivingEntity target, Location location, float basePower) {
		SpellCastState state = getCastState(caster);
		MagicSpells.debug(3, "Activating passive spell '" + name + "' for player " + caster.getName() + " (state: " + state + ")");
		if (!disabled && (chance >= .999 || random.nextFloat() <= chance) && state == SpellCastState.NORMAL) {
			disabled = true;
			SpellCastEvent event = new SpellCastEvent(this, caster, SpellCastState.NORMAL, basePower, null, this.cooldown, this.reagents.clone(), 0);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled() && event.getSpellCastState() == SpellCastState.NORMAL) {
				if (event.haveReagentsChanged() && !hasReagents(caster, event.getReagents())) {
					disabled = false;
					return false;
				}
				setCooldown(caster, event.getCooldown());
				basePower = event.getPower();
				boolean spellEffectsDone = false;
				for (Subspell spell : spells) {
					MagicSpells.debug(3, "    Casting spell effect '" + spell.getSpell().getName() + "'");
					if (castWithoutTarget) {
						MagicSpells.debug(3, "    Casting without target");
						spell.cast(caster, basePower);
						if (!spellEffectsDone) {
							playSpellEffects(EffectPosition.CASTER, caster);
							spellEffectsDone = true;
						}
					} else if (spell.isTargetedEntitySpell() && target != null && !isActuallyNonTargeted(spell.getSpell())) {
						MagicSpells.debug(3, "    Casting at entity");
						SpellTargetEvent targetEvent = new SpellTargetEvent(this, caster, target, basePower);
						Bukkit.getPluginManager().callEvent(targetEvent);
						if (!targetEvent.isCancelled()) {
							target = targetEvent.getTarget();
							spell.castAtEntity(caster, target, targetEvent.getPower());
							if (!spellEffectsDone) {
								playSpellEffects(caster, target);
								spellEffectsDone = true;
							}
						} else {
							MagicSpells.debug(3, "      Target cancelled (TE)");
						}
					} else if (spell.isTargetedLocationSpell() && (location != null || target != null)) {
						MagicSpells.debug(3, "    Casting at location");
						Location loc = null;
						if (location != null) {
							loc = location;
						} else if (target != null) {
							loc = target.getLocation();
						}
						if (loc != null) {
							SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, caster, loc, basePower);
							Bukkit.getPluginManager().callEvent(targetEvent);
							if (!targetEvent.isCancelled()) {
								loc = targetEvent.getTargetLocation();
								spell.castAtLocation(caster, loc, targetEvent.getPower());
								if (!spellEffectsDone) {
									playSpellEffects(caster, loc);
									spellEffectsDone = true;
								}
							} else {
								MagicSpells.debug(3, "      Target cancelled (TL)");
							}
						}
					} else {
						MagicSpells.debug(3, "    Casting normally");
						float power = basePower;
						if (target != null) {
							SpellTargetEvent targetEvent = new SpellTargetEvent(this, caster, target, power);
							Bukkit.getPluginManager().callEvent(targetEvent);
							if (!targetEvent.isCancelled()) {
								power = targetEvent.getPower();
							} else {
								MagicSpells.debug(3, "      Target cancelled (UE)");
								continue;
							}
						} else if (location != null) {
							SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, caster, location, basePower);
							Bukkit.getPluginManager().callEvent(targetEvent);
							if (!targetEvent.isCancelled()) {
								power = targetEvent.getPower();
							} else {
								MagicSpells.debug(3, "      Target cancelled (UL)");
								continue;
							}
						}
						spell.cast(caster, power);
						if (!spellEffectsDone) {
							playSpellEffects(EffectPosition.CASTER, caster);
							spellEffectsDone = true;
						}
					}
				}
				removeReagents(caster, event.getReagents());
				sendMessage(caster, strCastSelf);
				SpellCastedEvent event2 = new SpellCastedEvent(this, caster, SpellCastState.NORMAL, basePower, null, event.getCooldown(), event.getReagents(), PostCastAction.HANDLE_NORMALLY);
				Bukkit.getPluginManager().callEvent(event2);
				disabled = false;
				return true;
			} else {
				MagicSpells.debug(3, "   Passive spell canceled");
				disabled = false;
				return false;
			}
		} else if (state != SpellCastState.NORMAL && sendFailureMessages) {
			if (state == SpellCastState.ON_COOLDOWN) {
				MagicSpells.sendMessage(caster, formatMessage(strOnCooldown, "%c", Math.round(getCooldown(caster))+""));
			} else if (state == SpellCastState.MISSING_REAGENTS) {
				MagicSpells.sendMessage(caster, strMissingReagents);
				if (MagicSpells.showStrCostOnMissingReagents() && strCost != null && !strCost.isEmpty()) {
					MagicSpells.sendMessage(caster, "    (" + strCost + ")");
				}
			}
		}
		return false;
	}
	
	private boolean isActuallyNonTargeted(Spell spell) {
		if (spell instanceof ExternalCommandSpell) {
			return !((ExternalCommandSpell)spell).requiresPlayerTarget();
		} else if (spell instanceof BuffSpell) {
			return !((BuffSpell)spell).isTargeted();
		}
		return false;
	}
	
	public boolean cancelDefaultAction() {
		return cancelDefaultAction;
	}
	
	public boolean ignoreCancelled() {
		return ignoreCancelled;
	}
	
	@Override
	public boolean canBind(CastItem item) {
		return false;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}
	
	public static void resetManager() {
		if (manager != null) {
			manager.turnOff();
			manager = null;
		}
	}

}
