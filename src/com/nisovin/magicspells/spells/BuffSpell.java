package com.nisovin.magicspells.spells;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;

public abstract class BuffSpell extends Spell {
	
	protected ItemStack[] useCost;
	protected int healthCost = 0;
	protected int manaCost = 0;
	protected int hungerCost = 0;
	protected int experienceCost = 0;
	protected int levelsCost = 0;
	protected SpellReagents reagents;
	protected int useCostInterval;
	protected int numUses;
	protected int duration;
	protected boolean cancelOnRecast;
	protected String strFade;
	private boolean castWithItem;
	private boolean castByCommand;
	
	private HashMap<String,Integer> useCounter;
	private HashMap<String,Long> durationStartTime;
	
	public BuffSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> costList = getConfigStringList("use-cost", null);
		if (costList != null && costList.size() > 0) {
			useCost = new ItemStack [costList.size()];
			for (int i = 0; i < costList.size(); i++) {
				if (costList.get(i).contains(" ")) {
					String [] data = costList.get(i).split(" ");
					if (data[0].equalsIgnoreCase("health")) {
						healthCost = Integer.parseInt(data[1]);
					} else if (data[0].equalsIgnoreCase("mana")) {
						manaCost = Integer.parseInt(data[1]);
					} else if (data[0].equalsIgnoreCase("hunger")) {
						hungerCost = Integer.parseInt(data[1]);
					} else if (data[0].equalsIgnoreCase("experience")) {
						experienceCost = Integer.parseInt(data[1]);
					} else if (data[0].equalsIgnoreCase("levels")) {
						levelsCost = Integer.parseInt(data[1]);
					} else if (data[0].contains(":")) {
						String [] subdata = data[0].split(":");
						useCost[i] = new ItemStack(Integer.parseInt(subdata[0]), Integer.parseInt(data[1]), Short.parseShort(subdata[1]));
					} else {
						useCost[i] = new ItemStack(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
					}
				} else {
					useCost[i] = new ItemStack(Integer.parseInt(costList.get(i)));
				}
			}
		} else {
			useCost = null;
		}
		reagents = new SpellReagents(useCost, manaCost, healthCost, hungerCost, experienceCost, levelsCost);
		useCostInterval = getConfigInt("use-cost-interval", 0);
		numUses = getConfigInt("num-uses", 0);
		duration = getConfigInt("duration", 0);
		cancelOnRecast = getConfigBoolean("cancel-on-recast", true);
		
		strFade = getConfigString("str-fade", "");
		
		if (numUses > 0 || (useCost != null && useCostInterval > 0)) {
			useCounter = new HashMap<String,Integer>();
		}
		if (duration > 0) {
			durationStartTime = new HashMap<String,Long>();
		}
		
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
	}
	
	public boolean canCastWithItem() {
		return castWithItem;
	}
	
	public boolean canCastByCommand() {
		return castByCommand;
	}
	
	/**
	 * Begins counting the spell duration for a player
	 * @param player the player to begin counting duration
	 */
	protected void startSpellDuration(final Player player) {
		if (duration > 0 && durationStartTime != null) {
			durationStartTime.put(player.getName(), System.currentTimeMillis());
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					if (isExpired(player)) {
						turnOff(player);
					}
				}
			}, duration * 20 + 20); // overestimate ticks, since the duration is real-time ms based
		}
	}
	
	/**
	 * Checks whether the spell's duration has expired for a player
	 * @param player the player to check
	 * @return true if the spell has expired, false otherwise
	 */
	protected boolean isExpired(Player player) {
		if (duration <= 0 || durationStartTime == null) {
			return false;
		} else {
			Long startTime = durationStartTime.get(player.getName());
			if (startTime == null) {
				return false;
			} else if (startTime + duration*1000 > System.currentTimeMillis()) {
				return false;
			} else {
				return true;
			}			
		}
	}
	
	/**
	 * Adds a use to the spell for the player. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * This does not automatically charge the use cost.
	 * @param player the player to add the use for
	 * @return the player's current number of uses (returns 0 if the use counting feature is disabled)
	 */
	protected int addUse(Player player) {
		if (numUses > 0 || (useCost != null && useCostInterval > 0)) {
			Integer uses = useCounter.get(player.getName());
			if (uses == null) {
				uses = 1;
			} else {
				uses++;
			}
			
			if (numUses > 0 && uses >= numUses) {
				turnOff(player);
			} else {
				useCounter.put(player.getName(), uses);
			}
			return uses;
		} else {
			return 0;
		}
	}
	
	/**
	 * Removes this spell's use cost from the player's inventory. If the reagents aren't available, the spell will expire.
	 * @param player the player to remove the cost from
	 * @return true if the reagents were removed, or if the use cost is disabled, false otherwise
	 */
	protected boolean chargeUseCost(Player player) {
		if (useCost != null && useCostInterval > 0 && useCounter != null && useCounter.containsKey(player.getName())) {
			int uses = useCounter.get(player.getName());
			if (uses % useCostInterval == 0) {
				if (hasReagents(player, reagents)) {
					removeReagents(player, reagents);
					return true;
				} else {
					turnOff(player);
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Adds a use to the spell for the player. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * Removes this spell's use cost from the player's inventory. This does not return anything, to get useful return values, use
	 * addUse() and chargeUseCost().
	 * @param player the player to add a use and charge cost to
	 */
	protected void addUseAndChargeCost(Player player) {
		addUse(player);
		chargeUseCost(player);
	}
	
	/**
	 * Turns off this spell for the specified player. This can be called from many situations, including when the spell expires or the uses run out.
	 * When overriding this function, you should always be sure to call super.turnOff(player).
	 * @param player
	 */
	public void turnOff(Player player) {
		if (useCounter != null) useCounter.remove(player.getName());
		if (durationStartTime != null) durationStartTime.remove(player.getName());
	}
	
	@Override
	protected
	abstract void turnOff();
	
}