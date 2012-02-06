package com.nisovin.magicspells;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.minecraft.server.DataWatcher;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MobEffect;
import net.minecraft.server.Packet40EntityMetadata;
import net.minecraft.server.Packet42RemoveMobEffect;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;

public abstract class Spell implements Comparable<Spell>, Listener {

	private MagicConfig config;
	
	protected String internalName;
	protected String name;
	protected String[] aliases;
	
	protected String description;
	protected CastItem castItem;
	protected boolean bindable;
	protected HashSet<CastItem> bindableItems;
	protected int broadcastRange;
	
	protected ItemStack[] cost;
	protected int healthCost = 0;
	protected int manaCost = 0;
	protected int hungerCost = 0;
	protected int experienceCost = 0;
	protected int levelsCost = 0;
	
	protected int cooldown;
	protected HashMap<Spell, Integer> sharedCooldowns;
	protected boolean ignoreGlobalCooldown;
	
	protected String strCost;
	protected String strCastSelf;
	protected String strCastOthers;
	protected String strOnCooldown;
	protected String strMissingReagents;
	protected String strCantCast;
	protected String strCantBind;
	
	private HashMap<String, Long> lastCast;
	
	public Spell(MagicConfig config, String spellName) {
		this.config = config;
		
		this.internalName = spellName;
		loadConfigData(config, spellName, "spells");
		
	}
	
	protected void loadConfigData(MagicConfig config, String spellName, String section) {
		this.name = config.getString(section + "." + spellName + ".name", spellName);
		List<String> temp = config.getStringList(section + "." + spellName + ".aliases", null);
		if (temp != null) {
			aliases = new String[temp.size()];
			aliases = temp.toArray(aliases);
		}
		
		this.description = config.getString(section + "." + spellName + ".description", "");
		this.castItem = new CastItem(config.getString(section + "." + spellName + ".cast-item", "280"));
		this.bindable = config.getBoolean(section + "." + spellName + ".bindable", true);
		List<String> bindables = config.getStringList(section + "." + spellName + ".bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<CastItem>();
			for (String s : bindables) {
				bindableItems.add(new CastItem(s));
			}
		}
		this.broadcastRange = config.getInt(section + "." + spellName + ".broadcast-range", MagicSpells.broadcastRange);
		
		List<String> costList = config.getStringList(section + "." + spellName + ".cost", null);
		if (costList != null && costList.size() > 0) {
			cost = new ItemStack [costList.size()];
			String[] data, subdata;
			for (int i = 0; i < costList.size(); i++) {
				String costVal = costList.get(i);
				
				// validate cost data
				if (!costVal.matches("^([1-9][0-9]*(:[1-9][0-9]*)?|mana|health|hunger|experience|levels) [1-9][0-9]*$")) {
					MagicSpells.error(Level.WARNING, "Failed to process cost value for " + spellName + " spell: " + costVal);
					continue;
				}
				
				// parse cost data
				data = costVal.split(" ");
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
					subdata = data[0].split(":");
					cost[i] = new ItemStack(Integer.parseInt(subdata[0]), Integer.parseInt(data[1]), Short.parseShort(subdata[1]));
				} else {
					cost[i] = new ItemStack(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
				}
			}
		} else {
			cost = null;
		}
		
		this.cooldown = config.getInt(section + "." + spellName + ".cooldown", 0);
		List<String> cooldowns = config.getStringList(section + "." + spellName + ".shared-cooldowns", null);
		if (cooldowns != null) {
			this.sharedCooldowns = new HashMap<Spell,Integer>();
			for (String s : cooldowns) {
				String[] data = s.split(" ");
				Spell spell = MagicSpells.getSpellByInternalName(data[0]);
				int cd = Integer.parseInt(data[1]);
				if (spell != null) {
					this.sharedCooldowns.put(spell, cd);
				}
			}
		}
		this.ignoreGlobalCooldown = config.getBoolean(section + "." + spellName + ".ignore-global-cooldown", false);
		if (cooldown > 0) {
			lastCast = new HashMap<String, Long>();
		}
		
		this.strCost = config.getString(section + "." + spellName + ".str-cost", null);
		this.strCastSelf = config.getString(section + "." + spellName + ".str-cast-self", null);
		this.strCastOthers = config.getString(section + "." + spellName + ".str-cast-others", null);
		this.strOnCooldown = config.getString(section + "." + spellName + ".str-on-cooldown", MagicSpells.strOnCooldown);
		this.strMissingReagents = config.getString(section + "." + spellName + ".str-missing-reagents", MagicSpells.strMissingReagents);
		this.strCantCast = config.getString(section + "." + spellName + ".str-cant-cast", MagicSpells.strCantCast);
		this.strCantBind = config.getString(section + "." + spellName + ".str-cant-bind", null);
	}
	
	/**
	 * Access an integer config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected int getConfigInt(String key, int defaultValue) {
		return config.getInt("spells." + internalName + "." + key, defaultValue);
	}
	
	/**
	 * Access a boolean config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected boolean getConfigBoolean(String key, boolean defaultValue) {
		return config.getBoolean("spells." + internalName + "." + key, defaultValue);
	}
	
	/**
	 * Access a String config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected String getConfigString(String key, String defaultValue) {
		return config.getString("spells." + internalName + "." + key, defaultValue);
	}
	
	/**
	 * Access a float config value for this spell.
	 * 
	 * @param key The key of the config value
	 * @param defaultValue The value to return if it does not exist in the config
	 * 
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected float getConfigFloat(String key, float defaultValue) {
		return (float)config.getDouble("spells." + internalName + "." + key, defaultValue);
	}
	
	protected List<Integer> getConfigIntList(String key, List<Integer> defaultValue) {
		return config.getIntList("spells." + internalName + "." + key, defaultValue);
	}
	
	protected List<String> getConfigStringList(String key, List<String> defaultValue) {
		return config.getStringList("spells." + internalName + "." + key, defaultValue);
	}

	public final SpellCastResult cast(Player player) {
		return cast(player, null);
	}
	
	public final SpellCastResult cast(Player player, String[] args) {
		MagicSpells.debug("Player " + player.getName() + " is trying to cast " + internalName);
		
		// get spell state
		SpellCastState state;
		if (!MagicSpells.getSpellbook(player).canCast(this)) {
			state = SpellCastState.CANT_CAST;
		} else if (MagicSpells.noMagicZones != null && MagicSpells.noMagicZones.willFizzle(player, this)) {
			state = SpellCastState.NO_MAGIC_ZONE;
		} else if (onCooldown(player)) {
			state = SpellCastState.ON_COOLDOWN;
		} else if (!hasReagents(player)) {
			state = SpellCastState.MISSING_REAGENTS;
		} else {
			state = SpellCastState.NORMAL;
		}
		
		MagicSpells.debug("    Spell cast state: " + state);
		
		// call events
		float power = 1.0F;
		int cooldown = this.cooldown;
		SpellReagents reagents = new SpellReagents(cost, manaCost, healthCost, hungerCost, experienceCost, levelsCost);
		SpellCastEvent event = new SpellCastEvent(this, player, state, power, args, cooldown, reagents);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return new SpellCastResult(SpellCastState.CANT_CAST, PostCastAction.HANDLE_NORMALLY);
		} else {
			power = event.getPower();
			cooldown = event.getCooldown();
		}
		
		// cast spell
		PostCastAction action = castSpell(player, state, power, args);
		
		// perform post-cast action
		if (action != null && action != PostCastAction.ALREADY_HANDLED) {
			if (state == SpellCastState.NORMAL) {
				if (action == PostCastAction.HANDLE_NORMALLY || action == PostCastAction.COOLDOWN_ONLY || action == PostCastAction.NO_MESSAGES || action == PostCastAction.NO_REAGENTS) {
					setCooldown(player, cooldown);
				}
				if (action == PostCastAction.HANDLE_NORMALLY || action == PostCastAction.REAGENTS_ONLY || action == PostCastAction.NO_MESSAGES || action == PostCastAction.NO_COOLDOWN) {
					removeReagents(player, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels());
				}
				if (action == PostCastAction.HANDLE_NORMALLY || action == PostCastAction.MESSAGES_ONLY || action == PostCastAction.NO_COOLDOWN || action == PostCastAction.NO_REAGENTS) {
					sendMessage(player, strCastSelf);
					sendMessageNear(player, formatMessage(strCastOthers, "%a", player.getDisplayName()));
				}
			} else if (state == SpellCastState.ON_COOLDOWN) {
				MagicSpells.sendMessage(player, formatMessage(strOnCooldown, "%c", getCooldown(player)+""));
			} else if (state == SpellCastState.MISSING_REAGENTS) {
				MagicSpells.sendMessage(player, strMissingReagents);
				if (MagicSpells.showStrCostOnMissingReagents && strCost != null && !strCost.isEmpty()) {
					MagicSpells.sendMessage(player, "    (" + strCost + ")");
				}
			} else if (state == SpellCastState.CANT_CAST) {
				MagicSpells.sendMessage(player, strCantCast);
			} else if (state == SpellCastState.NO_MAGIC_ZONE) {
				MagicSpells.noMagicZones.sendNoMagicMessage(player, this);
			}
		}
		
		return new SpellCastResult(state, action);
	}

	/**
	 * This method is called when a player casts a spell, either by command, with a wand item, or otherwise.
	 * @param player the player casting the spell
	 * @param state the state of the spell cast (normal, on cooldown, missing reagents, etc)
	 * @param power the power multiplier the spell should be cast with (1.0 is normal)
	 * @param args the spell arguments, if cast by command
	 * @return the action to take after the spell is processed
	 */
	public abstract PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args);
	
	/**
	 * This method is called when the spell is cast from the console.
	 * @param sender the console sender.
	 * @param args the command arguments
	 * @return true if the spell was handled, false otherwise
	 */
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	public abstract boolean canCastWithItem();
	
	public abstract boolean canCastByCommand();
	
	public boolean canBind(CastItem item) {
		if (!bindable) {
			return false;
		} else if (bindableItems == null)  {
			return true;
		} else {
			return bindableItems.contains(item);
		}
	}
	
	public String getCostStr() {
		if (strCost == null || strCost.equals("")) {
			return null;
		} else {
			return strCost;
		}
	}
	
	/**
	 * Check whether this spell is currently on cooldown for the specified player
	 * @param player The player to check
	 * @return whether the spell is on cooldown
	 */
	public boolean onCooldown(Player player) {
		if (cooldown == 0 || player.hasPermission("magicspells.nocooldown")) {
			return false;
		}
		
		Long casted = lastCast.get(player.getName());
		if (casted != null) {
			if (casted + (cooldown*1000) > System.currentTimeMillis()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get how many seconds remain on the cooldown of this spell for the specified player
	 * @param player The player to check
	 * @return The number of seconds remaining in the cooldown
	 */
	public int getCooldown(Player player) {
		if (cooldown <= 0) {
			return 0;
		}
		
		Long casted = lastCast.get(player.getName());
		if (casted != null) {
			return (int)(cooldown - ((System.currentTimeMillis()-casted)/1000));
		} else {
			return 0;
		}
	}
	
	/**
	 * Begins the cooldown for the spell for the specified player
	 * @param player The player to set the cooldown for
	 */
	public void setCooldown(Player player, int cooldown) {
		setCooldown(player, cooldown, true);
	}
	
	/**
	 * Begins the cooldown for the spell for the specified player
	 * @param player The player to set the cooldown for
	 */
	public void setCooldown(Player player, int cooldown, boolean activateSharedCooldowns) {
		if (cooldown > 0) {
			lastCast.put(player.getName(), System.currentTimeMillis());
		}
		if (activateSharedCooldowns && sharedCooldowns != null) {
			for (Map.Entry<Spell, Integer> scd : sharedCooldowns.entrySet()) {
				scd.getKey().setCooldown(player, scd.getValue(), false);
			}
		}
	}
	
	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param player the player to check
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(Player player) {
		return hasReagents(player, cost, healthCost, manaCost, hungerCost, experienceCost, levelsCost);
	}
	
	/**
	 * Checks if a player has the specified reagents
	 * @param player the player to check
	 * @param cost the reagents to look for
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(Player player, ItemStack[] cost) {
		return hasReagents(player, cost, 0, 0, 0, 0, 0);
	}
	
	/**
	 * Checks if a player has the specified reagents, including health and mana
	 * @param player the player to check
	 * @param reagents the inventory item reagents to look for
	 * @param healthCost the health cost, in half-hearts
	 * @param manaCost the mana cost
	 * @return true if the player has all the reagents, false otherwise
	 */
	protected boolean hasReagents(Player player, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost) {
		if (player.hasPermission("magicspells.noreagents")) {
			return true;
		}
		if (reagents == null && healthCost <= 0 && manaCost <= 0 && hungerCost <= 0) {
			return true;
		}
		if (healthCost > 0 && player.getHealth() <= healthCost) { // TODO: add option to allow death from health cost
			return false;
		}
		if (manaCost > 0 && !MagicSpells.mana.hasMana(player, manaCost)) {
			return false;
		}
		if (hungerCost > 0 && player.getFoodLevel() < hungerCost) {
			return false;
		}
		if (experienceCost > 0 && player.getTotalExperience() < experienceCost) {
			return false;
		}
		if (levelsCost > 0 && player.getLevel() < levelsCost) {
			return false;
		}
		for (ItemStack item : reagents) {
			if (item != null && !inventoryContains(player.getInventory(), item)) {
				return false;
			}
		}
		return true;		
	}
	
	/**
	 * Removes the reagent cost of this spell from the player's inventoryy.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove reagents from
	 */
	protected void removeReagents(Player player) {
		removeReagents(player, cost, healthCost, manaCost, hungerCost, experienceCost, levelsCost);
	}
	
	/**
	 * Removes the specified reagents from the player's inventoryy.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 */
	protected void removeReagents(Player player, ItemStack[] reagents) {
		removeReagents(player, reagents, 0, 0, 0, 0, 0);
	}
	
	/**
	 * Removes the specified reagents, including health and mana, from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param player the player to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 * @param healthCost the health to remove
	 * @param manaCost the mana to remove
	 */
	protected void removeReagents(Player player, ItemStack[] reagents, int healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost) {
		if (player.hasPermission("magicspells.noreagents")) {
			return;
		}
		if (reagents != null) {
			for (ItemStack item : reagents) {
				if (item != null) {
					removeFromInventory(player.getInventory(), item);
				}
			}
		}
		if (healthCost > 0) {
			player.setHealth(player.getHealth() - healthCost);
		}
		if (manaCost > 0) {
			MagicSpells.mana.removeMana(player, manaCost);
		}
		if (hungerCost > 0) {
			player.setFoodLevel(player.getFoodLevel() - hungerCost);
		}
		if (experienceCost > 0) {
			player.setTotalExperience(player.getTotalExperience() - experienceCost);
		}
		if (levelsCost > 0) {
			int lvl = player.getLevel() - levelsCost;
			if (lvl < 0) lvl = 0;
			player.setLevel(lvl);
		}
	}
	
	private boolean inventoryContains(Inventory inventory, ItemStack item) {
		int count = 0;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				count += items[i].getAmount();
			}
			if (count >= item.getAmount()) {
				return true;
			}
		}
		return false;
	}
	
	private void removeFromInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				if (items[i].getAmount() > amt) {
					items[i].setAmount(items[i].getAmount() - amt);
					break;
				} else if (items[i].getAmount() == amt) {
					items[i] = null;
					break;
				} else {
					amt -= items[i].getAmount();
					items[i] = null;
				}
			}
		}
		inventory.setContents(items);
	}
	
	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	protected String formatMessage(String message, String... replacements) {
		return MagicSpells.formatMessage(message, replacements);
	}
	
	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(Player player, String message, String... replacements) {
		sendMessage(player, formatMessage(message, replacements));
	}
	
	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 */
	protected void sendMessage(Player player, String message) {
		MagicSpells.sendMessage(player, message);
	}
	
	/**
	 * Sends a message to all players near the specified player, within the configured broadcast range.
	 * @param player the "center" player used to find nearby players
	 * @param message the message to send
	 */
	protected void sendMessageNear(Player player, String message) {
		sendMessageNear(player, message, broadcastRange);
	}
	
	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param player the "center" player used to find nearby players
	 * @param message the message to send
	 * @param range the broadcast range
	 */
	protected void sendMessageNear(Player player, String message, int range) {
		if (message != null && !message.equals("") && !player.hasPermission("magicspells.silent")) {
			String [] msgs = message.replaceAll("&([0-9a-f])", "\u00A7$1").split("\n");
			List<Entity> entities = player.getNearbyEntities(range*2, range*2, range*2);
			for (Entity entity : entities) {
				if (entity instanceof Player && entity != player) {
					for (String msg : msgs) {
						if (!msg.equals("")) {
							((Player)entity).sendMessage(MagicSpells.textColor + msg);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Plays the magic-swirly potion effect on an entity for the player. The effect will only appear for the specified player.
	 * @param player the player to show the effect to
	 * @param entity the entity the effect will be on
	 * @param color the color of the swirls
	 * @param duration how long the effect will last, in ticks
	 */
	protected void playPotionEffect(final Player player, final LivingEntity entity, int color, int duration) {
		final DataWatcher dw = new DataWatcher();
		dw.a(8, Integer.valueOf(0));
		dw.watch(8, Integer.valueOf(color));
		
		Packet40EntityMetadata packet = new Packet40EntityMetadata(entity.getEntityId(), dw);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				DataWatcher dwReal = ((CraftLivingEntity)entity).getHandle().getDataWatcher();
				dw.watch(8, dwReal.getInt(8));
				Packet40EntityMetadata packet = new Packet40EntityMetadata(entity.getEntityId(), dw);
				((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
			}
		}, duration);
	}
	
	public void setMobEffect(LivingEntity entity, int type, int duration, int amplifier) {		
		((CraftLivingEntity)entity).getHandle().addEffect(new MobEffect(type, duration, amplifier));
	}
	
	@SuppressWarnings("rawtypes")
	public static void removeMobEffect(LivingEntity entity, int type) {
		try {
			// remove effect
			Field field = EntityLiving.class.getDeclaredField("effects");
			field.setAccessible(true);
			HashMap effects = (HashMap)field.get(((CraftLivingEntity)entity).getHandle());
			effects.remove(type);
			// alert player that effect is gone
			if (entity instanceof Player) {
				EntityPlayer player = ((CraftPlayer)entity).getHandle();
				player.netServerHandler.sendPacket(new Packet42RemoveMobEffect(player.id, new MobEffect(type, 0, 0)));
			}
			// remove graphical effect
			((CraftLivingEntity)entity).getHandle().getDataWatcher().watch(8, Integer.valueOf(0));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getInternalName() {
		return this.internalName;
	}
	
	public String getName() {
		if (this.name != null && !this.name.isEmpty()) {
			return this.name;
		} else {
			return this.internalName;
		}
	}
	
	public String getCantBindError() {
		return strCantBind;
	}
	
	public String[] getAliases() {
		return this.aliases;
	}
	
	public CastItem getCastItem() {
		return this.castItem;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public ItemStack[] getReagentCost() {
		return this.cost;
	}
	
	public int getManaCost() {
		return this.manaCost;
	}
	
	public int getHealthCost() {
		return this.healthCost;
	}
	
	public int getHungerCost() {
		return this.hungerCost;
	}
	
	public int getExperienceCost() {
		return this.experienceCost;
	}
	
	public int getLevelsCost() {
		return this.levelsCost;
	}
	
	/**
	 * This method is called immediately after all spells have been loaded.
	 */
	protected void initialize() {
	}
	
	/**
	 * This method is called when the plugin is being disabled, for any reason.
	 */
	protected void turnOff() {
	}
	
	@Override
	public int compareTo(Spell spell) {
		return this.name.compareTo(spell.name);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Spell && ((Spell)o).internalName.equals(this.internalName)) {
			return true;
		} else {
			return false;
		}		
	}
	
	@Override
	public int hashCode() {
		return internalName.hashCode();
	}
	
	public enum SpellCastState {
		NORMAL,
		ON_COOLDOWN,
		MISSING_REAGENTS,
		CANT_CAST,
		NO_MAGIC_ZONE
	}
	
	public enum PostCastAction {
		HANDLE_NORMALLY,
		ALREADY_HANDLED,
		NO_MESSAGES,
		NO_REAGENTS,
		NO_COOLDOWN,
		MESSAGES_ONLY,
		REAGENTS_ONLY,
		COOLDOWN_ONLY
	}
	
	public class SpellCastResult {
		public SpellCastState state;
		public PostCastAction action;
		public SpellCastResult(SpellCastState state, PostCastAction action) {
			this.state = state;
			this.action = action;
		}
	}

}
