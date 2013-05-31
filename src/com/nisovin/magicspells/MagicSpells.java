package com.nisovin.magicspells;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.events.MagicSpellsLoadingEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.mana.ManaSystem;
import com.nisovin.magicspells.util.ExperienceBarManager;
import com.nisovin.magicspells.util.ItemNameResolver;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicItemNameResolver;
import com.nisovin.magicspells.util.Metrics;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.Metrics.Graph;
import com.nisovin.magicspells.volatilecode.*;
import com.nisovin.magicspells.zones.NoMagicZoneManager;

public class MagicSpells extends JavaPlugin {

	public static MagicSpells plugin;

	static VolatileCodeHandle volatileCodeHandle;
	
	static boolean debug;
	static int debugLevel;
	static boolean enableErrorLogging;
	static boolean enableProfiling;
	static ChatColor textColor;
	static int broadcastRange;
	
	static boolean opsHaveAllSpells;
	static boolean defaultAllPermsFalse;
	
	static boolean separatePlayerSpellsPerWorld;
	static boolean allowCycleToNoSpell;
	static boolean alwaysShowMessageOnCycle;
	static boolean onlyCycleToCastableSpells;
	static int spellIconSlot;
	static boolean allowCastWithFist;
	static boolean castWithLeftClick;
	static boolean castWithRightClick;
	static boolean ignoreDefaultBindings;
	public static boolean showStrCostOnMissingReagents;
	static HashSet<Byte> losTransparentBlocks;
	static List<Integer> ignoreCastItemDurability;
	static HashMap<EntityType, String> entityNames;
	static int globalCooldown;
	static boolean castOnAnimate;
	static boolean useExpBarAsCastTimeBar;
	static boolean cooldownsPersistThroughReload;
	static boolean ignoreCastItemEnchants;
	static boolean ignoreCastItemNames;
	static boolean checkWorldPvpFlag;
	
	static boolean enableManaBars;
	static int manaPotionCooldown;
	static String strManaPotionOnCooldown;
	static HashMap<ItemStack, Integer> manaPotions;
	
	// strings
	static String strCastUsage;
	static String strUnknownSpell;
	static String strSpellChange;
	static String strSpellChangeEmpty;
	static String strOnCooldown;
	static String strMissingReagents;
	static String strCantCast;
	static String strWrongWorld;
	static String strCantBind;
	static String strConsoleName;
	
	// metrics vars
	static boolean metricsEnabled;
	static int metricSpellCasts;
	static int metricSpellCastsExternal;
	static int metricSpellCastsTargeted;
	static int metricSpellCastsInstant;
	static int metricSpellCastsBuff;
	static int metricErrors;
	
	// spell containers
	static HashMap<String, Spell> spells; // map internal names to spells
	static HashMap<String, Spell> spellNames; // map configured names to spells
	static ArrayList<Spell> spellsOrdered; // spells in loaded order
	static HashMap<String, Spellbook> spellbooks; // player spellbooks
	static HashMap<String, Spell> incantations; // map incantation strings to spells
		
	// container vars
	static ManaHandler mana;
	static HashMap<Player, Long> manaPotionCooldowns;
	static NoMagicZoneManager noMagicZones;
	static BuffManager buffManager;
	static ExperienceBarManager expBarManager;
	static ItemNameResolver itemNameResolver;
	
	// profiling
	static HashMap<String, Long> profilingTotalTime;
	static HashMap<String, Integer> profilingRuns;
	
	@Override
	public void onEnable() {
		plugin = this;		
		load();
	}
	
	void load() {
		PluginManager pm = plugin.getServer().getPluginManager();
		
		// create storage stuff
		spells = new HashMap<String,Spell>();
		spellNames = new HashMap<String,Spell>();
		spellsOrdered = new ArrayList<Spell>();
		spellbooks = new HashMap<String,Spellbook>();
		incantations = new HashMap<String,Spell>();
		
		// make sure directories are created
		this.getDataFolder().mkdir();
		new File(this.getDataFolder(), "spellbooks").mkdir();
		
		// load config
		if (!(new File(getDataFolder(), "config.yml")).exists() && !(new File(getDataFolder(), "general.yml")).exists()) {
			saveResource("general.yml", false);
			if (!(new File(getDataFolder(), "mana.yml")).exists()) saveResource("mana.yml", false);
			if (!(new File(getDataFolder(), "spells-command.yml")).exists()) saveResource("spells-command.yml", false);
			if (!(new File(getDataFolder(), "spells-regular.yml")).exists()) saveResource("spells-regular.yml", false);
			if (!(new File(getDataFolder(), "zones.yml")).exists()) saveResource("zones.yml", false);
		}
		MagicConfig config = new MagicConfig(this);
		if (!config.isLoaded()) {
			MagicSpells.log(Level.SEVERE, "Error in config file, stopping config load");
			return;
		}
		
		if (config.getBoolean("general.enable-volatile-features", true)) {
			try {
				Class.forName("net.minecraft.server.v1_5_R3.MinecraftServer");
				volatileCodeHandle = new VolatileCodeEnabled_1_5_R3();
			} catch (ClassNotFoundException e_1_5_r3) {
				try {
					Class.forName("net.minecraft.server.v1_5_R2.MinecraftServer");
					volatileCodeHandle = new VolatileCodeEnabled_1_5_R2();
				} catch (ClassNotFoundException e_1_5_r2) {
					error("This MagicSpells version is not fully compatible with this server version.");
					error("Some features have been disabled.");
					error("See http://nisovin.com/magicspells/volatilefeatures for more information.");
					volatileCodeHandle = new VolatileCodeDisabled();
				}
			}
		} else {
			volatileCodeHandle = new VolatileCodeDisabled();
		}
		
		debug = config.getBoolean("general.debug", false);
		debugLevel = config.getInt("general.debug-level", 3);
		enableErrorLogging = config.getBoolean("general.enable-error-logging", true);
		enableProfiling = config.getBoolean("general.enable-profiling", false);
		textColor = ChatColor.getByChar(config.getString("general.text-color", ChatColor.DARK_AQUA.getChar() + ""));
		broadcastRange = config.getInt("general.broadcast-range", 20);
		
		opsHaveAllSpells = config.getBoolean("general.ops-have-all-spells", true);
		defaultAllPermsFalse = config.getBoolean("general.default-all-perms-false", false);

		separatePlayerSpellsPerWorld = config.getBoolean("general.separate-player-spells-per-world", false);
		allowCycleToNoSpell = config.getBoolean("general.allow-cycle-to-no-spell", false);
		alwaysShowMessageOnCycle = config.getBoolean("general.always-show-message-on-cycle", false);
		onlyCycleToCastableSpells = config.getBoolean("general.only-cycle-to-castable-spells", true);
		spellIconSlot = config.getInt("general.spell-icon-slot", -1);
		allowCastWithFist = config.getBoolean("general.allow-cast-with-fist", false);
		castWithLeftClick = config.getBoolean("general.cast-with-left-click", true);
		castWithRightClick = config.getBoolean("general.cast-with-right-click", false);
		ignoreDefaultBindings = config.getBoolean("general.ignore-default-bindings", false);
		ignoreCastItemEnchants = config.getBoolean("general.ignore-cast-item-enchants", true);
		ignoreCastItemNames = config.getBoolean("general.ignore-cast-item-names", false);
		checkWorldPvpFlag = config.getBoolean("general.check-world-pvp-flag", false);
		showStrCostOnMissingReagents = config.getBoolean("general.show-str-cost-on-missing-reagents", true);
		losTransparentBlocks = new HashSet<Byte>(config.getByteList("general.los-transparent-blocks", new ArrayList<Byte>()));
		if (losTransparentBlocks.size() == 0) {
			losTransparentBlocks.add((byte)Material.AIR.getId());
			losTransparentBlocks.add((byte)Material.TORCH.getId());
			losTransparentBlocks.add((byte)Material.REDSTONE_WIRE.getId());
			losTransparentBlocks.add((byte)Material.REDSTONE_TORCH_ON.getId());
			losTransparentBlocks.add((byte)Material.REDSTONE_TORCH_OFF.getId());
			losTransparentBlocks.add((byte)Material.YELLOW_FLOWER.getId());
			losTransparentBlocks.add((byte)Material.RED_ROSE.getId());
			losTransparentBlocks.add((byte)Material.BROWN_MUSHROOM.getId());
			losTransparentBlocks.add((byte)Material.RED_MUSHROOM.getId());
			losTransparentBlocks.add((byte)Material.LONG_GRASS.getId());
			losTransparentBlocks.add((byte)Material.DEAD_BUSH.getId());
			losTransparentBlocks.add((byte)Material.DIODE_BLOCK_ON.getId());
			losTransparentBlocks.add((byte)Material.DIODE_BLOCK_OFF.getId());
		}
		ignoreCastItemDurability = config.getIntList("general.ignore-cast-item-durability", new ArrayList<Integer>());
		globalCooldown = config.getInt("general.global-cooldown", 500);
		castOnAnimate = config.getBoolean("general.cast-on-animate", false);
		useExpBarAsCastTimeBar = config.getBoolean("general.use-exp-bar-as-cast-time-bar", true);
		cooldownsPersistThroughReload = config.getBoolean("general.cooldowns-persist-through-reload", true);
		
		entityNames = new HashMap<EntityType, String>();
		if (config.contains("general.entity-names")) {
			Set<String> keys = config.getSection("general.entity-names").getKeys(false);
			for (String key : keys) {
				EntityType entityType = EntityType.fromName(key);
				if (entityType != null) {
					entityNames.put(entityType, config.getString("general.entity-names." + key, entityType.getName().toLowerCase()));
				}
			}
		}
		
		strCastUsage = config.getString("general.str-cast-usage", "Usage: /cast <spell>. Use /cast list to see a list of spells.");
		strUnknownSpell = config.getString("general.str-unknown-spell", "You do not know a spell with that name.");
		strSpellChange = config.getString("general.str-spell-change", "You are now using the %s spell.");
		strSpellChangeEmpty = config.getString("general.str-spell-change-empty", "You are no longer using a spell.");
		strOnCooldown = config.getString("general.str-on-cooldown", "That spell is on cooldown.");
		strMissingReagents = config.getString("general.str-missing-reagents", "You do not have the reagents for that spell.");
		strCantCast = config.getString("general.str-cant-cast", "You can't cast that spell right now.");
		strCantBind = config.getString("general.str-cant-bind", "You cannot bind that spell to that item.");
		strWrongWorld = config.getString("general.str-wrong-world", "You cannot cast that spell here.");
		strConsoleName = config.getString("general.console-name", "Admin");
		
		enableManaBars = config.getBoolean("mana.enable-mana-system", false);
		manaPotionCooldown = config.getInt("mana.mana-potion-cooldown", 30);
		strManaPotionOnCooldown = config.getString("mana.str-mana-potion-on-cooldown", "You cannot use another mana potion yet.");
		
		// create handling objects
		if (enableManaBars) mana = new ManaSystem(config);
		noMagicZones = new NoMagicZoneManager();
		buffManager = new BuffManager(config.getInt("general.buff-check-interval", 0));
		expBarManager = new ExperienceBarManager();
		itemNameResolver = new MagicItemNameResolver();
		
		// call loading event
		pm.callEvent(new MagicSpellsLoadingEvent(this));
				
		// init permissions
		boolean opsIgnoreReagents = config.getBoolean("general.ops-ignore-reagents", true);
		boolean opsIgnoreCooldowns = config.getBoolean("general.ops-ignore-cooldowns", true);
		boolean opsIgnoreCastTimes = config.getBoolean("general.ops-ignore-cast-times", true);
		addPermission(pm, "noreagents", opsIgnoreReagents? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without needing reagents");
		addPermission(pm, "nocooldown", opsIgnoreCooldowns? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without being affected by cooldowns");
		addPermission(pm, "nocasttime", opsIgnoreCastTimes? PermissionDefault.OP : PermissionDefault.FALSE, "Allows casting without being affected by cast times");
		addPermission(pm, "notarget", PermissionDefault.FALSE, "Prevents being targeted by any targeted spells");
		addPermission(pm, "silent", PermissionDefault.FALSE, "Prevents cast messages from being broadcast to players");
		HashMap<String, Boolean> permGrantChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permLearnChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permCastChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permTeachChildren = new HashMap<String,Boolean>();
		
		// load predefined items
		Util.predefinedItems.clear();
		if (config.contains("general.predefined-items")) {
			Set<String> predefinedItems = config.getKeys("general.predefined-items");
			if (predefinedItems != null) {
				for (String key : predefinedItems) {
					String s = config.getString("general.predefined-items." + key, null);
					if (s != null) {
						ItemStack is = Util.getItemStackFromString(s);
						if (is != null) {
							Util.predefinedItems.put(key, is);
						} else {
							MagicSpells.error("Invalid predefined item: " + s);
						}
					}
				}
			}
		}
		
		// load spells
		loadSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
		log("Spells loaded: " + spells.size());
		if (spells.size() == 0) {
			MagicSpells.error("No spells loaded!");
			return;
		}
		
		// finalize spell permissions
		addPermission(pm, "grant.*", PermissionDefault.FALSE, permGrantChildren);
		addPermission(pm, "learn.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permLearnChildren);
		addPermission(pm, "cast.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permCastChildren);
		addPermission(pm, "teach.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permTeachChildren);
		
		// advanced perms
		addPermission(pm, "advanced.list", PermissionDefault.FALSE);
		addPermission(pm, "advanced.forget", PermissionDefault.FALSE);
		addPermission(pm, "advanced.scroll", PermissionDefault.FALSE);
		HashMap<String, Boolean> advancedPermChildren = new HashMap<String,Boolean>();
		advancedPermChildren.put("magicspells.advanced.list", true);
		advancedPermChildren.put("magicspells.advanced.forget", true);
		advancedPermChildren.put("magicspells.advanced.scroll", true);
		addPermission(pm, "advanced.*", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP, advancedPermChildren);
		
		// load in-game spell names, incantations, and initialize spells
		for (Spell spell : spells.values()) {
			spellNames.put(spell.getName().toLowerCase(), spell);
			String[] aliases = spell.getAliases();
			if (aliases != null && aliases.length > 0) {
				for (String alias : aliases) {
					if (!spellNames.containsKey(alias.toLowerCase())) {
						spellNames.put(alias.toLowerCase(), spell);
					}
				}
			}
			List<String> incs = spell.getIncantations();
			if (incs != null && incs.size() > 0) {
				for (String s : incs) {
					incantations.put(s.toLowerCase(), spell);
				}
			}
			spell.initialize();
		}
		
		// load online player spellbooks
		for (Player p : getServer().getOnlinePlayers()) {
			spellbooks.put(p.getName(), new Spellbook(p, this));
		}
		
		// load saved cooldowns
		if (cooldownsPersistThroughReload) {
			File file = new File(getDataFolder(), "cooldowns.txt");
			Scanner scanner = null;
			if (file.exists()) {
				try {
					scanner = new Scanner(file);
					while (scanner.hasNext()) {
						String line = scanner.nextLine();
						if (!line.isEmpty()) {
							String[] data = line.split(":");
							long cooldown = Long.parseLong(data[2]);
							if (cooldown > System.currentTimeMillis()) {
								Spell spell = getSpellByInternalName(data[0]);
								if (spell != null) {
									spell.setCooldownManually(data[1], cooldown);
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (scanner != null) scanner.close();
					file.delete();
				}
			}
		}
		
		// setup mana
		if (enableManaBars) {
			// setup online player mana bars
			for (Player p : getServer().getOnlinePlayers()) {
				mana.createManaBar(p);
			}
			
			// load mana potions
			List<String> manaPots = config.getStringList("mana.mana-potions", null);
			if (manaPots != null && manaPots.size() > 0) {
				manaPotions = new LinkedHashMap<ItemStack,Integer>();
				for (int i = 0; i < manaPots.size(); i++) {
					String[] data = manaPots.get(i).split(" ");
					if (data.length == 2 && data[1].matches("^[0-9]+$")) {
						ItemStack item = Util.getItemStackFromString(data[0]);
						if (item != null) {
							manaPotions.put(item, Integer.parseInt(data[1]));
						} else {
							error("Invalid mana potion: " + manaPots.get(i));
						}
					} else {
						error("Invalid mana potion: " + manaPots.get(i));
					}
				}
				manaPotionCooldowns = new HashMap<Player,Long>();
			}
		}
		
		// load no-magic zones
		noMagicZones.load(config);
		if (noMagicZones.zoneCount() == 0) {
			noMagicZones = null;
		}
		
		// load listeners
		registerEvents(new MagicPlayerListener(this));
		registerEvents(new MagicSpellListener(this));
		if (config.getBoolean("general.enable-new-spell-handling", false)) {
			registerEvents(new CastListenerNew(this));
		} else {
			registerEvents(new CastListener(this));
		}
		if (incantations.size() > 0) {
			registerEvents(new MagicChatListener(this));
		}
		
		// register commands
		CastCommand exec = new CastCommand(this, config.getBoolean("general.enable-tab-completion", true));
		getCommand("magicspellcast").setExecutor(exec);
		getCommand("magicspellmana").setExecutor(exec);
		//if (config.getBoolean("general.enable-tab-completion", true)) {
		//	getCommand("magicspellcast").setTabCompleter(exec);
		//}
		
		// setup metrics
		metricsEnabled = false;//config.getBoolean("general.enable-stat-collection", true);
		if (metricsEnabled) {
			setupMetrics();
		}
		
		// setup profiling
		if (enableProfiling) {
			profilingTotalTime = new HashMap<String, Long>();
			profilingRuns = new HashMap<String, Integer>();
		}
		
		// call loaded event
		pm.callEvent(new MagicSpellsLoadedEvent(this));
	}
	
	private void loadSpells(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// load spells from plugin folder
		final List<File> jarList = new ArrayList<File>();
		for (File file : getDataFolder().listFiles()) {
			if (file.getName().endsWith(".jar")) {
				jarList.add(file);
			}
		}

		// create class loader
		URL[] urls = new URL[jarList.size()+1];
		ClassLoader cl = getClassLoader();
		try {		
			urls[0] = getDataFolder().toURI().toURL();
			for(int i = 1; i <= jarList.size(); i++) {
				urls[i] = jarList.get(i-1).toURI().toURL();
			}
			cl = new URLClassLoader(urls, getClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		// get spells from config
		Set<String> spellKeys = config.getSpellKeys();
		if (spellKeys == null) return;
		for (String spellName : spellKeys) {
			if (config.getBoolean("spells." + spellName + ".enabled", true)) {
				String className = "";
				if (config.contains("spells." + spellName + ".spell-class")) {
					className = config.getString("spells." + spellName + ".spell-class", "");
				}
				if (className == null || className.isEmpty()) {
					error("Spell '" + spellName + "' does not have a spell-class property");
					continue;
				} else if (className.startsWith(".")) {
					className = "com.nisovin.magicspells.spells" + className;
				}
				try {
					// load spell class
					Class<? extends Spell> spellClass = cl.loadClass(className).asSubclass(Spell.class);
					Constructor<? extends Spell> constructor = spellClass.getConstructor(MagicConfig.class, String.class);
					constructor.setAccessible(true);
					Spell spell = constructor.newInstance(config, spellName);
					spells.put(spellName.toLowerCase(), spell);
					spellsOrdered.add(spell);
					
					// add permissions
					addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
					addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "tempgrant." + spellName, PermissionDefault.FALSE);
					permGrantChildren.put("magicspells.grant." + spellName, true);
					permLearnChildren.put("magicspells.learn." + spellName, true);
					permCastChildren.put("magicspells.cast." + spellName, true);
					permTeachChildren.put("magicspells.teach." + spellName, true);
					
					// done
					debug(2, "Loaded spell: " + spellName);
					
				} catch (ClassNotFoundException e) {
					error("Unable to load spell " + spellName + " (missing class " + className + ")");
				} catch (NoSuchMethodException e) {
					error("Unable to load spell " + spellName + " (malformed class)");
				} catch (Exception e) {
					error("Unable to load spell " + spellName + " (general error)");
					e.printStackTrace();
				}
			}
		}
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault) {
		addPermission(pm, perm, permDefault, null, null);
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, String description) {
		addPermission(pm, perm, permDefault, null, description);
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, Map<String,Boolean> children) {
		addPermission(pm, perm, permDefault, children, null);
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, Map<String,Boolean> children, String description) {
		if (pm.getPermission("magicspells." + perm) == null) {
			if (description == null) {
				pm.addPermission(new Permission("magicspells." + perm, permDefault, children));
			} else {
				pm.addPermission(new Permission("magicspells." + perm, description, permDefault, children));
			}
		}
	}
	
	private void setupMetrics() {
		try {
			Metrics metrics = new Metrics(this);
			
			Graph graph1 = metrics.createGraph("Spell Casts");
			graph1.addPlotter(new Metrics.Plotter("Total") {
				@Override
				public int getValue() {
					int r = metricSpellCasts;
					metricSpellCasts = 0;
					return r;
				}
			});
			graph1.addPlotter(new Metrics.Plotter("External") {
				@Override
				public int getValue() {
					int r = metricSpellCastsExternal;
					metricSpellCastsExternal = 0;
					return r;
				}
			});
			graph1.addPlotter(new Metrics.Plotter("Targeted") {
				@Override
				public int getValue() {
					int r = metricSpellCastsTargeted;
					metricSpellCastsTargeted = 0;
					return r;
				}
			});
			graph1.addPlotter(new Metrics.Plotter("Instant") {
				@Override
				public int getValue() {
					int r = metricSpellCastsInstant;
					metricSpellCastsInstant = 0;
					return r;
				}
			});
			graph1.addPlotter(new Metrics.Plotter("Buff") {
				@Override
				public int getValue() {
					int r = metricSpellCastsBuff;
					metricSpellCastsBuff = 0;
					return r;
				}
			});
			
			Graph graph2 = metrics.createGraph("Errors");
			graph2.addPlotter(new Metrics.Plotter("Errors") {
				@Override
				public int getValue() {
					int r = metricErrors;
					metricErrors = 0;
					return r;
				}
			});
			
			metrics.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the instance of the MagicSpells plugin
	 * @return the MagicSpells plugin
	 */
	public static MagicSpells getInstance() {
		return plugin;
	}
	
	public static Collection<Spell> spells() {
		return spells.values();
	}
	
	/**
	 * Gets a spell by its internal name (the key name in the config file)
	 * @param spellName the internal name of the spell to find
	 * @return the Spell found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInternalName(String spellName) {
		return spells.get(spellName.toLowerCase());
	}
	
	/**
	 * Gets a spell by its in-game name (the name specified with the 'name' config option)
	 * @param spellName the in-game name of the spell to find
	 * @return the Spell found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInGameName(String spellName) {
		return spellNames.get(spellName.toLowerCase());
	}
	
	/**
	 * Gets a player's spellbook, which contains known spells and handles spell permissions. 
	 * If a player does not have a spellbook, one will be created.
	 * @param player the player to get a spellbook for
	 * @return the player's spellbook
	 */
	public static Spellbook getSpellbook(Player player) {
		Spellbook spellbook = spellbooks.get(player.getName());
		if (spellbook == null) {
			spellbook = new Spellbook(player, plugin);
			spellbooks.put(player.getName(), spellbook);
		}
		return spellbook;
	}
	
	public static ChatColor getTextColor() {
		return textColor;
	}
	
	/**
	 * Gets a list of blocks that are considered transparent
	 * @return list of block types
	 */
	public static HashSet<Byte> getTransparentBlocks() {
		return losTransparentBlocks;
	}
	
	/**
	 * Gets a map of entity types and their configured names, to be used when sending messages to players
	 * @return the map
	 */
	public static HashMap<EntityType, String> getEntityNames() {
		return entityNames;
	}
	
	/**
	 * Checks whether to ignore the durability on the given type when using it as a cast item.
	 * @param type the type to check
	 * @return whether to ignore durability
	 */
	public static boolean ignoreCastItemDurability(int type) {
		if (ignoreCastItemDurability != null && ignoreCastItemDurability.contains(type)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean ignoreCastItemEnchants() {
		return ignoreCastItemEnchants;
	}
	
	public static boolean ignoreCastItemNames() {
		return ignoreCastItemNames;
	}
	
	/**
	 * Gets the handler for no-magic zones.
	 * @return the no-magic zone handler
	 */
	public static NoMagicZoneManager getNoMagicZoneManager() {
		return noMagicZones;
	}
	
	public static BuffManager getBuffManager() {
		return buffManager;
	}
	
	/**
	 * Gets the mana handler, which handles all mana transactions.
	 * @return the mana handler
	 */
	public static ManaHandler getManaHandler() {
		return mana;
	}
	
	/**
	 * Sets the mana handler, which handles all mana transactions.
	 * @param handler the mana handler
	 */
	public static void setManaHandler(ManaHandler handler) {
		mana.turnOff();
		mana = handler;
	}
	
	public static VolatileCodeHandle getVolatileCodeHandler() {
		return volatileCodeHandle;
	}
	
	public static ExperienceBarManager getExpBarManager() {
		return expBarManager;
	}
	
	public static ItemNameResolver getItemNameResolver() {
		return itemNameResolver;
	}
	
	public static void setItemNameResolver(ItemNameResolver resolver) {
		itemNameResolver = resolver;
	}
	
	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	static public String formatMessage(String message, String... replacements) {
		if (message == null) return null;
		
		String msg = message;
		for (int i = 0; i < replacements.length; i+=2) {
			if (replacements[i] != null) {
				if (replacements[i+1] != null) {
					msg = msg.replace(replacements[i], replacements[i+1]);
				} else {
					msg = msg.replace(replacements[i], "");
				}
			}
		}
		return msg;
	}
	
	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	static public void sendMessage(Player player, String message, String... replacements) {
		sendMessage(player, formatMessage(message, replacements));
	}
	
	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 */
	static public void sendMessage(Player player, String message) {
		if (message != null && !message.equals("")) {
			String [] msgs = message.replaceAll("&([0-9a-fk-or])", "\u00A7$1").split("\n");
			for (String msg : msgs) {
				if (!msg.equals("")) {
					player.sendMessage(MagicSpells.textColor + msg);
				}
			}
		}
	}
	
	static void registerEvents(final Listener listener) {
		Method[] methods;
        try {
            methods = listener.getClass().getDeclaredMethods();
        } catch (NoClassDefFoundError e) {
            return;
        }
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            final EventHandler eh = method.getAnnotation(EventHandler.class);
            if (eh == null) continue;
            final Class<?> checkClass = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(checkClass) || method.getParameterTypes().length != 1) {
                plugin.getLogger().severe("Wrong method arguments used for event type registered");
                continue;
            }
            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);
            EventExecutor executor = new EventExecutor() {
            	final String eventKey = enableProfiling ? "Event:" + listener.getClass().getName().replace("com.nisovin.magicspells.","") + "." + method.getName() + "(" + eventClass.getSimpleName() + ")" : null;
                public void execute(Listener listener, Event event) {
                    try {
                        if (!eventClass.isAssignableFrom(event.getClass())) {
                            return;
                        }
                        long start = System.nanoTime();
                        method.invoke(listener, event);
                        if (enableProfiling) {
                        	Long total = profilingTotalTime.get(eventKey);
                        	if (total == null) total = (long)0;
                        	total += (System.nanoTime() - start);
                        	profilingTotalTime.put(eventKey, total);
                        	Integer runs = profilingRuns.get(eventKey);
                        	if (runs == null) runs = 0;
                        	runs += 1;
                        	profilingRuns.put(eventKey, runs);
                        }
                    } catch (Exception ex) {
                    	handleException(ex);
                    }
                }
            };
            plugin.getServer().getPluginManager().registerEvent(eventClass, listener, eh.priority(), executor, plugin, eh.ignoreCancelled());
        }
	}
	
	public static int scheduleDelayedTask(final Runnable task, int delay) {
		return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, !enableErrorLogging ? task : new Runnable() {
			public void run() {
				try {
					task.run();
				} catch (Exception e) {
					handleException(e);
				}
			}
		}, delay);
	}
	
	public static int scheduleRepeatingTask(final Runnable task, int delay, int interval) {
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, !enableErrorLogging ? task : new Runnable() {
			public void run() {
				try {
					task.run();
				} catch (Exception e) {
					handleException(e);
				}
			}
		}, delay, interval);
	}
	
	public static void cancelTask(int taskId) {
		Bukkit.getScheduler().cancelTask(taskId);
	}
	
	public static void handleException(Exception ex) {
		if (enableErrorLogging) {
			plugin.getLogger().severe("AN EXCEPTION HAS OCCURED:");
			PrintWriter writer = null;
			try {
				File folder = new File(plugin.getDataFolder(), "errors");
				if (!folder.exists()) folder.mkdir();
				writer = new PrintWriter(new File(folder, System.currentTimeMillis() + ".txt"));
				Throwable t = ex;
				while (t != null) {
					plugin.getLogger().severe("    " + t.getMessage() + " (" + t.getClass().getName() + ")");
					t.printStackTrace(writer);
					writer.println();
					t = t.getCause();
				}
				plugin.getLogger().severe("This error has been saved in the errors folder");
				writer.println("Server version: " + Bukkit.getServer().getVersion());
				writer.println("MagicSpells version: " + plugin.getDescription().getVersion());
			} catch (Exception x) {
				plugin.getLogger().severe("ERROR HANDLING EXCEPTION");
				x.printStackTrace();
				ex.printStackTrace();
			} finally {
				if (writer != null) writer.close();
			}
		} else {
			ex.printStackTrace();
		}
		metricErrors++;
	}
	
	static void profilingReport() {
		if (profilingTotalTime != null && profilingRuns != null) {
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new File(plugin.getDataFolder(), "profiling_report_" + System.currentTimeMillis() + ".txt"));
				long totalTime = 0;
				writer.println("Key\tRuns\tAvg\tTotal");
				for (String key : profilingTotalTime.keySet()) {
					long time = profilingTotalTime.get(key);
					int runs = profilingRuns.get(key);
					totalTime += time;
					writer.println(key + "\t" + runs + "\t" + (time/runs/1000000F) + "ms\t" + (time/1000000F) + "ms");
				}
				writer.println();
				writer.println("TOTAL TIME: " + (totalTime/1000000F) + "ms");
			} catch (Exception ex) {
				error("Failed to save profiling report");
				handleException(ex);
			} finally {
				if (writer != null) writer.close();
			}
			profilingTotalTime.clear();
			profilingRuns.clear();
		}
	}
	
	/**
	 * Writes a debug message to the console if the debug option is enabled.
	 * Uses debug level 2.
	 * @param message the message to write to the console
	 */
	public static void debug(String message) {
		debug(2, message);
	}
	
	/**
	 * Writes a debug message to the console if the debug option is enabled.
	 * @param level the debug level to log with
	 * @param message the message to write to the console
	 */
	public static void debug(int level, String message) {
		if (MagicSpells.debug && level <= debugLevel) {
			log(Level.INFO, message);
		}
	}
	
	public static void log(String message) {
		log(Level.INFO, message);
	}
	
	public static void error(String message) {
		log(Level.WARNING, message);
	}
	
	/**
	 * Writes an error message to the console.
	 * @param level the error level
	 * @param message the error message
	 */
	public static void log(Level level, String message) {
		plugin.getLogger().log(level, message);
	}
	
	/**
	 * Teaches a player a spell (adds it to their spellbook)
	 * @param player the player to teach
	 * @param spellName the spell name, either the in-game name or the internal name
	 * @return whether the spell was taught to the player
	 */
	public static boolean teachSpell(Player player, String spellName) {
		Spell spell = spellNames.get(spellName.toLowerCase());
		if (spell == null) {
			spell = spells.get(spellName.toLowerCase());
			if (spell == null) {
				return false;
			}
		}
		
		Spellbook spellbook = getSpellbook(player);
		
		if (spellbook == null || spellbook.hasSpell(spell) || !spellbook.canLearn(spell)) {
			return false;
		} else {
			// call event
			SpellLearnEvent event = new SpellLearnEvent(spell, player, LearnSource.OTHER, null);
			plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			} else {
				spellbook.addSpell(spell);
				spellbook.save();
				return true;
			}
		}
	}
	
	void unload() {
		// turn off spells
		for (Spell spell : spells.values()) {
			spell.turnOff();
		}
		
		// save cooldowns
		if (cooldownsPersistThroughReload) {
			File file = new File(getDataFolder(), "cooldowns.txt");
			if (file.exists()) file.delete();
			try {
				FileWriter writer = new FileWriter(file);
				for (Spell spell : spells.values()) {
					Map<String, Long> cooldowns = spell.getCooldowns();
					for (String name : cooldowns.keySet()) {
						long cooldown = cooldowns.get(name);
						if (cooldown > System.currentTimeMillis()) {
							writer.append(spell.getInternalName() + ":" + name + ":" + cooldown + "\n");
						}
					}
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				file.delete();
			}
		}
		
		// turn off buff manager
		if (buffManager != null) {
			buffManager.turnOff();
			buffManager = null;
		}
		
		// clear memory
		spells.clear();
		spells = null;
		spellNames.clear();
		spellNames = null;
		spellbooks.clear();
		spellbooks = null;
		incantations.clear();
		incantations = null;
		if (mana != null) {
			mana.turnOff();
			mana = null;
		}
		if (manaPotionCooldowns != null) {
			manaPotionCooldowns.clear();
			manaPotionCooldowns = null;
		}
		if (noMagicZones != null) {
			noMagicZones.turnOff();
			noMagicZones = null;
		}
		expBarManager = null;
		itemNameResolver = null;
		
		// remove star permissions (to allow new spells to be added to them)
		getServer().getPluginManager().removePermission("magicspells.grant.*");
		getServer().getPluginManager().removePermission("magicspells.cast.*");
		getServer().getPluginManager().removePermission("magicspells.learn.*");
		getServer().getPluginManager().removePermission("magicspells.teach.*");
		
		// unregister all listeners
		HandlerList.unregisterAll(this);
		
		// cancel all tasks
		Bukkit.getScheduler().cancelTasks(this);
	}
	
	@Override
	public void onDisable() {		
		unload();
	}
	
}

/*
 * TODO for version 3.0:
 * 
 * - Use MagicPlayer (Caster/PlayerCaster) across the entire plugin
 * - Allow spells to be cast by something other than players, like blocks and other entities
 * - Create a method for task scheduling and use it across the entire plugin
 * - Make BuffSpell handle more stuff automatically
 *   - Implement castSpell
 *   - Handle toggling
 *   - Start spell duration
 * - Move NoMagicZoneWorldGuard and NoMagicZoneResidence outside of the core plugin
 * - Remove default strings from all string configs (default should be empty)
 * 
 */