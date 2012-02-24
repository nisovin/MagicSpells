package com.nisovin.magicspells;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.spells.*;
import com.nisovin.magicspells.spells.buff.*;
import com.nisovin.magicspells.spells.channeled.*;
import com.nisovin.magicspells.spells.command.*;
import com.nisovin.magicspells.spells.instant.*;
import com.nisovin.magicspells.spells.targeted.BlinkSpell;
import com.nisovin.magicspells.spells.targeted.BuildSpell;
import com.nisovin.magicspells.spells.targeted.CombustSpell;
import com.nisovin.magicspells.spells.targeted.CrippleSpell;
import com.nisovin.magicspells.spells.targeted.DisarmSpell;
import com.nisovin.magicspells.spells.targeted.DrainlifeSpell;
import com.nisovin.magicspells.spells.targeted.EntombSpell;
import com.nisovin.magicspells.spells.targeted.ExplodeSpell;
import com.nisovin.magicspells.spells.targeted.FireballSpell;
import com.nisovin.magicspells.spells.targeted.ForcetossSpell;
import com.nisovin.magicspells.spells.targeted.GeyserSpell;
import com.nisovin.magicspells.spells.targeted.HealSpell;
import com.nisovin.magicspells.spells.targeted.LightningSpell;
import com.nisovin.magicspells.spells.targeted.PainSpell;
import com.nisovin.magicspells.spells.targeted.PotionEffectSpell;
import com.nisovin.magicspells.spells.targeted.TelekinesisSpell;
import com.nisovin.magicspells.spells.targeted.VolleySpell;
import com.nisovin.magicspells.spells.targeted.ZapSpell;
import com.nisovin.magicspells.util.CraftBukkitHandle;
import com.nisovin.magicspells.util.CraftBukkitHandleDisabled;
import com.nisovin.magicspells.util.CraftBukkitHandleEnabled;
import com.nisovin.magicspells.util.MagicConfig;

public class MagicSpells extends JavaPlugin {

	public static MagicSpells plugin;

	public static CraftBukkitHandle craftbukkit;
	
	protected static boolean debug;
	protected static ChatColor textColor;
	protected static int broadcastRange;
	
	protected static boolean opsHaveAllSpells;
	protected static boolean defaultAllPermsFalse;
	
	protected static boolean allowCycleToNoSpell;
	protected static boolean onlyCycleToCastableSpells;
	protected static boolean ignoreDefaultBindings;
	protected static boolean showStrCostOnMissingReagents;
	public static List<Integer> losTransparentBlocks;
	public static List<Integer> ignoreCastItemDurability;
	protected static int globalCooldown;
	protected static boolean castOnAnimate;
	
	protected static boolean enableManaBars;
	protected static int manaPotionCooldown;
	protected static String strManaPotionOnCooldown;
	protected static HashMap<ItemStack,Integer> manaPotions;
	
	protected static String strCastUsage;
	protected static String strUnknownSpell;
	protected static String strSpellChange;
	protected static String strSpellChangeEmpty;
	public static String strOnCooldown;
	public static String strMissingReagents;
	protected static String strCantCast;
	protected static String strNoMagicZone;
	protected static String strCantBind;
	public static String strConsoleName;
	
	protected static HashMap<String,Spell> spells; // map internal names to spells
	protected static HashMap<String,Spell> spellNames; // map configured names to spells
	protected static HashMap<String,Spellbook> spellbooks; // player spellbooks
	
	protected static ManaHandler mana;
	protected static HashMap<Player,Long> manaPotionCooldowns;
	public static NoMagicZoneHandler noMagicZones;
	
	@Override
	public void onEnable() {
		plugin = this;		
		load();
	}
	
	private void load() {		
		// load listeners
		PluginManager pm = plugin.getServer().getPluginManager();
		pm.registerEvents(new MagicPlayerListener(this), this);
		pm.registerEvents(new MagicSpellListener(this), this);
		
		// create storage stuff
		spells = new HashMap<String,Spell>();
		spellNames = new HashMap<String,Spell>();
		spellbooks = new HashMap<String,Spellbook>();
		
		// make sure directories are created
		this.getDataFolder().mkdir();
		new File(this.getDataFolder(), "spellbooks").mkdir();
		
		// load config
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) saveDefaultConfig();
		MagicConfig config = new MagicConfig(configFile);
		
		if (config.getBoolean("general.enable-volatile-features", true)) {
			craftbukkit = new CraftBukkitHandleEnabled();
		} else {
			craftbukkit = new CraftBukkitHandleDisabled();
		}
		
		debug = config.getBoolean("general.debug", false);
		textColor = ChatColor.getByChar(config.getString("general.text-color", ChatColor.DARK_AQUA.getChar() + ""));
		broadcastRange = config.getInt("general.broadcast-range", 20);
		
		opsHaveAllSpells = config.getBoolean("general.ops-have-all-spells", true);
		defaultAllPermsFalse = config.getBoolean("general.default-all-perms-false", false);

		allowCycleToNoSpell = config.getBoolean("general.allow-cycle-to-no-spell", false);
		onlyCycleToCastableSpells = config.getBoolean("general.only-cycle-to-castable-spells", true);
		ignoreDefaultBindings = config.getBoolean("general.ignore-default-bindings", false);
		showStrCostOnMissingReagents = config.getBoolean("general.show-str-cost-on-missing-reagents", true);
		losTransparentBlocks = config.getIntList("general.los-transparent-blocks", null);
		if (losTransparentBlocks == null || losTransparentBlocks.size() == 0) {
			losTransparentBlocks = new ArrayList<Integer>();
			losTransparentBlocks.add(Material.AIR.getId());
			losTransparentBlocks.add(Material.TORCH.getId());
			losTransparentBlocks.add(Material.REDSTONE_WIRE.getId());
			losTransparentBlocks.add(Material.REDSTONE_TORCH_ON.getId());
			losTransparentBlocks.add(Material.REDSTONE_TORCH_OFF.getId());
			losTransparentBlocks.add(Material.YELLOW_FLOWER.getId());
			losTransparentBlocks.add(Material.RED_ROSE.getId());
			losTransparentBlocks.add(Material.BROWN_MUSHROOM.getId());
			losTransparentBlocks.add(Material.RED_MUSHROOM.getId());
			losTransparentBlocks.add(Material.LONG_GRASS.getId());
			losTransparentBlocks.add(Material.DEAD_BUSH.getId());
			losTransparentBlocks.add(Material.DIODE_BLOCK_ON.getId());
			losTransparentBlocks.add(Material.DIODE_BLOCK_OFF.getId());
		}
		ignoreCastItemDurability = config.getIntList("general.ignore-cast-item-durability", new ArrayList<Integer>());
		globalCooldown = config.getInt("general.global-cooldown", 500);
		castOnAnimate = config.getBoolean("general.cast-on-animate", true);
		
		strCastUsage = config.getString("general.str-cast-usage", "Usage: /cast <spell>. Use /cast list to see a list of spells.");
		strUnknownSpell = config.getString("general.str-unknown-spell", "You do not know a spell with that name.");
		strSpellChange = config.getString("general.str-spell-change", "You are now using the %s spell.");
		strSpellChangeEmpty = config.getString("general.str-spell-change-empty", "You are no longer using a spell.");
		strOnCooldown = config.getString("general.str-on-cooldown", "That spell is on cooldown.");
		strMissingReagents = config.getString("general.str-missing-reagents", "You do not have the reagents for that spell.");
		strCantCast = config.getString("general.str-cant-cast", "You can't cast that spell right now.");
		strCantBind = config.getString("general.str-cant-bind", "You cannot bind that spell to that item.");
		strNoMagicZone = config.getString("general.str-no-magic-zone", "An anti-magic aura makes your spell fizzle.");
		strConsoleName = config.getString("general.console-name", "Admin");
		
		enableManaBars = config.getBoolean("general.mana.enable-mana-bars", true);
		manaPotionCooldown = config.getInt("general.mana.mana-potion-cooldown", 30);
		strManaPotionOnCooldown = config.getString("general.mana.str-mana-potion-on-cooldown", "You cannot use another mana potion yet.");
		
		boolean useNewLoading = config.getBoolean("general.use-new-spell-loading", false);
		
		// setup mana bar manager
		if (enableManaBars) {
			mana = new ManaBarManager(config);
			for (Player p : getServer().getOnlinePlayers()) {
				mana.createManaBar(p);
			}
		}
		
		// load mana potions
		List<String> manaPots = config.getStringList("general.mana.mana-potions", null);
		if (manaPots != null && manaPots.size() > 0) {
			manaPotions = new HashMap<ItemStack,Integer>();
			for (int i = 0; i < manaPots.size(); i++) {
				String[] data = manaPots.get(i).split(" ");
				ItemStack item;
				if (data[0].contains(":")) {
					String[] data2 = data[0].split(":");
					item = new ItemStack(Integer.parseInt(data2[0]), 1, Short.parseShort(data2[1]));
				} else {
					item = new ItemStack(Integer.parseInt(data[0]), 1);					
				}
				manaPotions.put(item, Integer.parseInt(data[1]));
			}
			manaPotionCooldowns = new HashMap<Player,Long>();
		}
		
		// load no-magic zones
		noMagicZones = new NoMagicZoneManager(config);
		if (noMagicZones.zoneCount() == 0) {
			noMagicZones = null;
		}
		
		// load permissions
		addPermission(pm, "noreagents", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP);
		addPermission(pm, "nocooldown", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP);
		addPermission(pm, "notarget", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP);
		HashMap<String, Boolean> permGrantChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permLearnChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permCastChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permTeachChildren = new HashMap<String,Boolean>();
		
		// load spells
		if (useNewLoading) {
			loadSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
		} else {
			loadNormalSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
			loadCustomSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
			loadSpellCopies(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);			
		}
		loadMultiSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
		
		// finalize permissions
		addPermission(pm, "grant.*", PermissionDefault.FALSE, permGrantChildren);
		addPermission(pm, "learn.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permLearnChildren);
		addPermission(pm, "cast.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permCastChildren);
		addPermission(pm, "teach.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permTeachChildren);
		
		// load in-game spell names and initialize spells
		for (Spell spell : spells.values()) {
			spellNames.put(spell.getName(), spell);
			String[] aliases = spell.getAliases();
			if (aliases != null && aliases.length > 0) {
				for (String alias : aliases) {
					if (!spellNames.containsKey(alias)) {
						spellNames.put(alias, spell);
					}
				}
			}
			spell.initialize();
			pm.registerEvents(spell, this);
		}
		
		// load online player spellbooks
		for (Player p : getServer().getOnlinePlayers()) {
			spellbooks.put(p.getName(), new Spellbook(p, this));
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
		for (String spellName : spellKeys) {
			String className = "";
			if (config.contains("spells." + spellName + ".spell-class")) {
				className = config.getString("spells." + spellName + ".spell-class", "");
			}
			if (className == null || className.isEmpty()) {
				getLogger().warning("Spell '" + spellName + "' does not have a spell-class property");
				continue;
			} else if (className.startsWith(".")) {
				className = "com.nisovin.magicspells.spells" + className;
			}
			if (config.getBoolean("spells." + spellName + ".enabled", true)) {
				try {
					// load spell class
					Class<? extends Spell> spellClass = cl.loadClass(className).asSubclass(Spell.class); // Class.forName(className, true, cl).asSubclass(Spell.class);
					Constructor<? extends Spell> constructor = spellClass.getConstructor(MagicConfig.class, String.class);
					Spell spell = constructor.newInstance(config, spellName);
					spells.put(spellName, spell);
					
					// add permissions
					addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
					addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					permGrantChildren.put("magicspells.grant." + spellName, true);
					permLearnChildren.put("magicspells.learn." + spellName, true);
					permCastChildren.put("magicspells.cast." + spellName, true);
					permTeachChildren.put("magicspells.teach." + spellName, true);
					
					// done
					debug("Loaded spell: " + spellName);
					
				} catch (ClassNotFoundException e) {
					getLogger().severe("Unable to load spell " + spellName + " (missing class " + className + ")");
					if (className.contains("instant")) {
						getLogger().severe("(Maybe try " + className.replace("com.nisovin.magicspells.spells.instant.", ".targeted.") + ")");
					}
				} catch (NoSuchMethodException e) {
					getLogger().severe("Unable to load spell " + spellName + " (malformed class)");
				} catch (Exception e) {
					getLogger().severe("Unable to load spell " + spellName + " (unknown error)");
					e.printStackTrace();
				}
			}
		}
	}
	
	private void loadNormalSpells(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// create list of spells
		ArrayList<Class<? extends Spell>> spellClasses = new ArrayList<Class<? extends Spell>>();
		spellClasses.add(BindSpell.class);
		spellClasses.add(BlinkSpell.class);
		spellClasses.add(BuildSpell.class);
		spellClasses.add(CarpetSpell.class);
		spellClasses.add(CombustSpell.class);
		spellClasses.add(ConfusionSpell.class);
		spellClasses.add(ConjureSpell.class);
		spellClasses.add(CrippleSpell.class);
		spellClasses.add(DisarmSpell.class);
		spellClasses.add(DrainlifeSpell.class);
		spellClasses.add(EmpowerSpell.class);
		spellClasses.add(EntombSpell.class);
		spellClasses.add(ExplodeSpell.class);
		spellClasses.add(ExternalCommandSpell.class);
		spellClasses.add(FireballSpell.class);
		spellClasses.add(FirenovaSpell.class);
		spellClasses.add(FlamewalkSpell.class);
		spellClasses.add(ForcepushSpell.class);
		spellClasses.add(ForcetossSpell.class);
		spellClasses.add(ForgetSpell.class);
		spellClasses.add(FrostwalkSpell.class);
		spellClasses.add(GateSpell.class);
		spellClasses.add(GeyserSpell.class);
		spellClasses.add(GillsSpell.class);
		spellClasses.add(HasteSpell.class);
		spellClasses.add(HealSpell.class);
		spellClasses.add(HelpSpell.class);
		spellClasses.add(InvulnerabilitySpell.class);
		spellClasses.add(LeapSpell.class);
		spellClasses.add(LifewalkSpell.class);
		spellClasses.add(LightningSpell.class);
		spellClasses.add(LightwalkSpell.class);
		spellClasses.add(ListSpell.class);
		spellClasses.add(ManaSpell.class);
		spellClasses.add(MarkSpell.class);
		spellClasses.add(MinionSpell.class);
		spellClasses.add(PainSpell.class);
		spellClasses.add(PermissionSpell.class);
		spellClasses.add(PhaseSpell.class);
		spellClasses.add(PotionEffectSpell.class);
		spellClasses.add(PrayerSpell.class);
		spellClasses.add(PurgeSpell.class);
		spellClasses.add(ReachSpell.class);
		spellClasses.add(RecallSpell.class);
		spellClasses.add(ReflectSpell.class);
		spellClasses.add(RepairSpell.class);
		spellClasses.add(ScrollSpell.class);
		spellClasses.add(SpellbookSpell.class);
		spellClasses.add(StealthSpell.class);
		spellClasses.add(StonevisionSpell.class);
		spellClasses.add(SummonSpell.class);
		spellClasses.add(SunSpell.class);
		spellClasses.add(TeachSpell.class);
		spellClasses.add(TelekinesisSpell.class);
		if (getServer().getPluginManager().isPluginEnabled("BookWorm")) spellClasses.add(TomeSpell.class);
		spellClasses.add(VolleySpell.class);
		spellClasses.add(WalkwaySpell.class);
		spellClasses.add(WallSpell.class);
		spellClasses.add(WindwalkSpell.class);
		spellClasses.add(ZapSpell.class);
		// load the spells
		for (Class<? extends Spell> c : spellClasses) {
			try {
				// get spell name
				String spellName;
				try {
					Field spellNameField = c.getDeclaredField("SPELL_NAME");
					spellNameField.setAccessible(true);
					spellName = (String)spellNameField.get(null);
				} catch (NoSuchFieldException e) {
					spellName = c.getSimpleName().replace("Spell", "").toLowerCase(); 
				} catch (IllegalAccessException e) {
					spellName = c.getSimpleName().replace("Spell", "").toLowerCase(); 	
				}
				// check enabled
				if (config.getBoolean("spells." + spellName + ".enabled", true)) {
					// initialize spell
					Constructor<? extends Spell> constructor = c.getConstructor(MagicConfig.class, String.class);
					Spell spell = constructor.newInstance(config, spellName);
					spells.put(spellName, spell);
					// add permissions
					addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
					addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					permGrantChildren.put("magicspells.grant." + spellName, true);
					permLearnChildren.put("magicspells.learn." + spellName, true);
					permCastChildren.put("magicspells.cast." + spellName, true);
					permTeachChildren.put("magicspells.teach." + spellName, true);
					// spell load complete
					debug("Loaded spell: " + spellName);
				}
			} catch (Exception e) {
				getServer().getLogger().severe("MagicSpells: Failed to load spell: " + c.getName());
				e.printStackTrace();
			}
		}
		
	}
	
	private void loadCustomSpells(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// load spells from plugin folder
		final List<File> jarList = new ArrayList<File>();
		File[] classFiles = getDataFolder().listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.endsWith(".class")) {
					if (name.contains("$")) {
						return false;
					} else {
						return true;
					}
				} else if (name.endsWith(".jar")) {
					jarList.add(new File(dir, name));
					return true;
				} else {
					return false;
				}
			}			
		});
		try {
			// generate URL list for look up
			URL[] urls = new URL[jarList.size()+1];
			
			// first URL is data folder itself for .class files
			urls[0] = getDataFolder().toURI().toURL();
			
			// other URLs are Jar files
			for(int i = 1; i <= jarList.size(); i++) {
				urls[i] = jarList.get(i-1).toURI().toURL();
			}
			
			// load the classes
			URLClassLoader ucl = new URLClassLoader(urls, getClassLoader());
			for (File file : classFiles) {
				try {
					
					// load spell from class file
					String fileName = file.getName().replaceAll("\\.[\\p{javaLetter}]+$", "");
					Class<? extends Spell> c = Class.forName(fileName, true, ucl).asSubclass(Spell.class);
					
					// get spell name
					String spellName;
					try {
						Field spellNameField = c.getDeclaredField("SPELL_NAME");
						spellNameField.setAccessible(true);
						spellName = (String)spellNameField.get(null);
					} catch (NoSuchFieldException e) {
						spellName = c.getSimpleName().replace("Spell", "").toLowerCase(); 
					} catch (IllegalAccessException e) {
						spellName = c.getSimpleName().replace("Spell", "").toLowerCase(); 						
					}
					
					// load the spell
					if (config.getBoolean("spells." + spellName + ".enabled", true)) {
						// initialize spell
						Spell spell = c.getConstructor(MagicConfig.class, String.class).newInstance(config, spellName);
						spells.put(spellName, spell);
						// add permissions
						addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
						addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
						addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
						addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
						permGrantChildren.put("magicspells.grant." + spellName, true);
						permLearnChildren.put("magicspells.learn." + spellName, true);
						permCastChildren.put("magicspells.cast." + spellName, true);
						permTeachChildren.put("magicspells.teach." + spellName, true);
						// spell load complete
						debug("Loaded external spell: " + spellName);
					}
				} catch (Exception e) {
					getServer().getLogger().severe("MagicSpells: Failed to load external spell: " + file.getName());
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			getServer().getLogger().severe("MagicSpells: Failed to create external spells");
			e.printStackTrace();
		}
	}
	
	private void loadSpellCopies(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// load spell copies
		List<String> copies = config.getStringList("spellcopies", new ArrayList<String>());
		List<String> moreCopies = config.getStringList("spells.spellcopies", null);
		if (moreCopies != null && copies != null) {
			copies.addAll(moreCopies);
		}
		if (copies != null && copies.size() > 0) {
			for (String copy : copies) {
				String[] data = copy.split("=");
				Spell spell = spells.get(data[1]);
				String spellName = data[0];
				if (spell != null) {
					try {
						// check enabled
						if (config.getBoolean("spells." + spellName + ".enabled", true)) {
							// initialize spell
							Spell spellCopy = spell.getClass().getConstructor(MagicConfig.class, String.class).newInstance(config, spellName);
							spells.put(spellName, spellCopy);
							// add permissions
							addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
							addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
							addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
							addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
							permGrantChildren.put("magicspells.grant." + spellName, true);
							permLearnChildren.put("magicspells.learn." + spellName, true);
							permCastChildren.put("magicspells.cast." + spellName, true);
							permTeachChildren.put("magicspells.teach." + spellName, true);
							// load complete
							debug("Loaded spell copy: " + data[0] + " (copy of " + data[1] + ")");
						}
					} catch (Exception e) {
						getServer().getLogger().severe("MagicSpells: Failed to create spell copy: " + copy);
					}
				}
			}
		}
	}
	
	private void loadMultiSpells(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// load multi-spells
		Set<String> multiSpells = config.getKeys("multispells");
		if (multiSpells != null && multiSpells.size() > 0) {
			for (String spellName : multiSpells) {
				if (config.getBoolean("multispells." + spellName + ".enabled", true)) {
					// initialize spell
					OldMultiSpell multiSpell = new OldMultiSpell(config, spellName);
					spells.put(spellName, multiSpell);
					// add permissions
					addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
					addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					permGrantChildren.put("magicspells.grant." + spellName, true);
					permLearnChildren.put("magicspells.learn." + spellName, true);
					permCastChildren.put("magicspells.cast." + spellName, true);
					permTeachChildren.put("magicspells.teach." + spellName, true);
					// load complete
					debug("Loaded multi-spell: " + spellName);
				}
			}
		}
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault) {
		addPermission(pm, perm, permDefault, null);
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, Map<String,Boolean> children) {
		if (pm.getPermission("magicspells." + perm) == null) {
			pm.addPermission(new Permission("magicspells." + perm, permDefault, children));
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String [] args) {
		if (command.getName().equalsIgnoreCase("magicspellcast")) {
			if (args == null || args.length == 0) {
				if (sender instanceof Player) {
					sendMessage((Player)sender, strCastUsage);
				} else {
					sender.sendMessage(textColor + strCastUsage);
				}
			} else if (sender.isOp() && args[0].equals("reload")) {
				if (args.length == 1) {
					unload();
					load();
					sender.sendMessage(textColor + "MagicSpells config reloaded.");
				} else {
					List<Player> players = getServer().matchPlayer(args[1]);
					if (players.size() != 1) {
						sender.sendMessage(textColor + "Player not found.");
					} else {
						Player player = players.get(0);
						spellbooks.put(player.getName(), new Spellbook(player, this));
						sender.sendMessage(textColor + player.getName() + "'s spellbook reloaded.");
					}
				}
			} else if (sender.isOp() && args[0].equals("debug")) {
				debug = !debug;
				sender.sendMessage("MagicSpells: debug mode " + (debug?"enabled":"disabled"));
			} else if (sender instanceof Player) {
				Player player = (Player)sender;
				Spellbook spellbook = getSpellbook(player);
				Spell spell = getSpellByInGameName(args[0]);
				if (spell != null && spell.canCastByCommand() && spellbook.hasSpell(spell)) {
					String[] spellArgs = null;
					if (args.length > 1) {
						spellArgs = new String[args.length-1];
						for (int i = 1; i < args.length; i++) {
							spellArgs[i-1] = args[i];
						}
					}
					spell.cast(player, spellArgs);
				} else {
					sendMessage(player, strUnknownSpell);
				}
			} else { // not a player
				Spell spell = spellNames.get(args[0]);
				if (spell == null) {
					sender.sendMessage("Unknown spell.");
				} else {
					String[] spellArgs = null;
					if (args.length > 1) {
						spellArgs = new String[args.length-1];
						for (int i = 1; i < args.length; i++) {
							spellArgs[i-1] = args[i];
						}
					}
					boolean ok = spell.castFromConsole(sender, spellArgs);
					if (!ok) {
						sender.sendMessage("Cannot cast that spell from console.");
					}
				}
			}
			return true;
		} else if (command.getName().equalsIgnoreCase("magicspellmana")) {
			if (enableManaBars && sender instanceof Player) {
				Player player = (Player)sender;
				mana.showMana(player, true);
			}
			return true;
		}
		return false;
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
		return spells.get(spellName);
	}
	
	/**
	 * Gets a spell by its in-game name (the name specified with the 'name' config option)
	 * @param spellName the in-game name of the spell to find
	 * @return the Spell found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInGameName(String spellName) {
		return spellNames.get(spellName);
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
	
	public static ManaHandler getManaHandler() {
		return mana;
	}
	
	public static void setManaHandler(ManaHandler m) {
		mana.turnOff();
		mana = m;
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
			msg = msg.replace(replacements[i], replacements[i+1]);
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
			String [] msgs = message.replaceAll("&([0-9a-f])", "\u00A7$1").split("\n");
			for (String msg : msgs) {
				if (!msg.equals("")) {
					player.sendMessage(MagicSpells.textColor + msg);
				}
			}
		}
	}
	
	/**
	 * Writes a debug message to the console if the debug option is enabled.
	 * @param message the message to write to the console
	 */
	public static void debug(String message) {
		if (MagicSpells.debug) {
			plugin.getServer().getLogger().info("MagicSpells: " + message);
		}
	}
	
	/**
	 * Writes an error message to the console.
	 * @param level the error level
	 * @param message the error message
	 */
	public static void error(Level level, String message) {
		plugin.getServer().getLogger().log(level, "MagicSpells: " + message);
	}
	
	/**
	 * Teaches a player a spell (adds it to their spellbook)
	 * @param player the player to teach
	 * @param spellName the spell name, either the in-game name or the internal name
	 * @return whether the spell was taught to the player
	 */
	public static boolean teachSpell(Player player, String spellName) {
		Spell spell = spellNames.get(spellName);
		if (spell == null) {
			spell = spells.get(spellName);
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
	
	public void unload() {
		for (Spell spell : spells.values()) {
			spell.turnOff();
		}
		spells.clear();
		spells = null;
		spellNames.clear();
		spellNames = null;
		spellbooks.clear();
		spellbooks = null;
		if (mana != null) {
			mana.turnOff();
			mana = null;
		}
		HandlerList.unregisterAll(this);	
	}
	
	@Override
	public void onDisable() {		
		unload();
	}
	
}
