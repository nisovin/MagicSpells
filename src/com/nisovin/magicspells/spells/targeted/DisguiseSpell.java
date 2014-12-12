package com.nisovin.magicspells.spells.targeted;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class DisguiseSpell extends TargetedSpell implements TargetedEntitySpell {

	static DisguiseManager manager;

	private DisguiseSpell thisSpell;
	private EntityType entityType;
	private boolean flag = false;
	private int var1 = 0;
	private int var2 = 0;
	private int var3 = 0;
	private boolean showPlayerName = false;
	private String nameplateText = "";
	private String uuid = "";
	private String skin = "";
	private String skinSig = "";
	private boolean alwaysShowNameplate = true;
	private boolean preventPickups = false;
	private boolean friendlyMobs = true;
	private boolean ridingBoat = false;
	private boolean undisguiseOnDeath = true;
	private boolean undisguiseOnLogout = false;
	private boolean undisguiseOnCast = false;
	private boolean undisguiseOnGiveDamage = false;
	private boolean undisguiseOnTakeDamage = false;
	private int duration;
	private boolean toggle;
	private String strFade;
	
	private Map<String, Disguise> disguised = new HashMap<String, Disguise>();
	
	public DisguiseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		thisSpell = this;
		
		if (manager == null) {
			try {
				manager = MagicSpells.getVolatileCodeHandler().getDisguiseManager(config);
			} catch (Exception e) {
				manager = null;
			}
			if (manager == null) {
				MagicSpells.error("DisguiseManager could not be created!");
				return;
			}
		}
		manager.registerSpell(this);
		
		String type = getConfigString("entity-type", "zombie");
		if (type.startsWith("baby ")) {
			flag = true;
			type = type.replace("baby ", "");
		}
		if (type.equalsIgnoreCase("human") || type.equalsIgnoreCase("player")) {
			type = "player";
		} else if (type.equalsIgnoreCase("wither skeleton")) {
			type = "skeleton";
			flag = true;
		} else if (type.equalsIgnoreCase("zombie villager") || type.equalsIgnoreCase("villager zombie")) {
			type = "zombie";
			var1 = 1;
		} else if (type.equalsIgnoreCase("powered creeper")) {
			type = "creeper";
			flag = true;
		} else if (type.toLowerCase().startsWith("villager ")) {
			String prof = type.toLowerCase().replace("villager ", "");
			if (prof.matches("^[0-5]$")) {
				var1 = Integer.parseInt(prof);
			} else if (prof.toLowerCase().startsWith("green")) {
				var1 = 5;
			} else {
				try {
					var1 = Profession.valueOf(prof.toUpperCase()).getId();
				} catch (Exception e) {
					MagicSpells.error("Invalid villager profession on disguise spell '" + spellName + "'");
				}
			}
			type = "villager";
		} else if (type.toLowerCase().endsWith(" villager")) {
			String prof = type.toLowerCase().replace(" villager", "");
			if (prof.toLowerCase().startsWith("green")) {
				var1 = 5;
			} else {
				try {
					var1 = Profession.valueOf(prof.toUpperCase()).getId();
				} catch (Exception e) {
					MagicSpells.error("Invalid villager profession on disguise spell '" + spellName + "'");
				}
			}
			type = "villager";
		} else if (type.toLowerCase().endsWith(" sheep")) {
			String color = type.toLowerCase().replace(" sheep", "");
			if (color.equalsIgnoreCase("random")) {
				var1 = -1;
			} else {
				try {
					DyeColor dyeColor = DyeColor.valueOf(color.toUpperCase().replace(" ", "_"));
					if (dyeColor != null) {
						var1 = dyeColor.getWoolData();
					}
				} catch (IllegalArgumentException e) {
					MagicSpells.error("Invalid sheep color on disguise spell '" + spellName + "'");
				}
			}
			type = "sheep";
		} else if (type.toLowerCase().endsWith(" rabbit")) {
			String rabbitType = type.toLowerCase().replace(" rabbit", "");
			var1 = 0;
			if (rabbitType.equals("white")) {
				var1 = 1;
			} else if (rabbitType.equals("black")) {
				var1 = 2;
			} else if (rabbitType.equals("blackwhite")) {
				var1 = 3;
			} else if (rabbitType.equals("gold")) {
				var1 = 4;
			} else if (rabbitType.equals("saltpepper")) {
				var1 = 5;
			} else if (rabbitType.equals("killer")) {
				var1 = 99;
			}
			type = "rabbit";
		} else if (type.toLowerCase().startsWith("wolf ")) {
			String color = type.toLowerCase().replace("wolf ", "");
			if (color.matches("[0-9a-fA-F]+")) {
				var1 = Integer.parseInt(color, 16);
			}
			type = "wolf";
		} else if (type.toLowerCase().equalsIgnoreCase("saddled pig")) {
			var1 = 1;
			type = "pig";
		} else if (type.equalsIgnoreCase("irongolem")) {
			type = "villagergolem";
		} else if (type.equalsIgnoreCase("mooshroom")) {
			type = "mushroomcow";
		} else if (type.equalsIgnoreCase("magmacube")) {
			type = "lavaslime";
		} else if (type.toLowerCase().contains("ocelot")) {
			type = type.toLowerCase().replace("ocelot", "ozelot");
		} else if (type.equalsIgnoreCase("snowgolem")) {
			type = "snowman";
		} else if (type.equalsIgnoreCase("wither")) {
			type = "witherboss";
		} else if (type.equalsIgnoreCase("dragon")) {
			type = "enderdragon";
		} else if (type.toLowerCase().startsWith("block") || type.toLowerCase().startsWith("fallingblock")) {
			String data = type.split(" ")[1];
			if (data.contains(":")) {
				String[] subdata = data.split(":");
				var1 = Integer.parseInt(subdata[0]);
				var2 = Integer.parseInt(subdata[1]);
			} else {
				var1 = Integer.parseInt(data);
			}
			type = "fallingsand";
		} else if (type.toLowerCase().startsWith("item")) {
			String data = type.split(" ")[1];
			if (data.contains(":")) {
				String[] subdata = data.split(":");
				var1 = Integer.parseInt(subdata[0]);
				var2 = Integer.parseInt(subdata[1]);
			} else {
				var1 = Integer.parseInt(data);
			}
			type = "item";
		} else if (type.toLowerCase().contains("horse")) {
			List<String> data = new ArrayList<String>(Arrays.asList(type.split(" ")));
			var1 = 0;
			var2 = 0;
			if (data.get(0).equalsIgnoreCase("horse")) {
				data.remove(0);
			} else if (data.size() >= 2 && data.get(1).equalsIgnoreCase("horse")) {
				String t = data.remove(0).toLowerCase();
				if (t.equals("donkey")) {
					var1 = 1;
				} else if (t.equals("mule")) {
					var1 = 2;
				} else if (t.equals("skeleton") || t.equals("skeletal")) {
					var1 = 4;
				} else if (t.equals("zombie") || t.equals("undead")) {
					var1 = 3;
				} else {
					var1 = 0;
				}
				data.remove(0);
			}
			while (data.size() > 0) {
				String d = data.remove(0);
				if (d.matches("^[0-9]+$")) {
					var2 = Integer.parseInt(d);
				} else if (d.equalsIgnoreCase("iron")) {
					var3 = 1;
				} else if (d.equalsIgnoreCase("gold")) {
					var3 = 2;
				} else if (d.equalsIgnoreCase("diamond")) {
					var3 = 3;
				}
			}
			type = "entityhorse";
		} else if (type.equalsIgnoreCase("mule")) {
			var1 = 2;
			type = "entityhorse";
		} else if (type.equalsIgnoreCase("donkey")) {
			var1 = 1;
			type = "entityhorse";
		} else if (type.equalsIgnoreCase("elder guardian")) {
			flag = true;
			type = "guardian";
		}
		if (type.toLowerCase().matches("ozelot [0-3]")) {
			var1 = Integer.parseInt(type.split(" ")[1]);
			type = "ozelot";
		} else if (type.toLowerCase().equals("ozelot random") || type.toLowerCase().equals("random ozelot")) {
			var1 = -1;
			type = "ozelot";
		}
		if (type.equals("player")) {
			entityType = EntityType.PLAYER;
		} else {
			entityType = EntityType.fromName(type);
		}
		showPlayerName = getConfigBoolean("show-player-name", false);
		nameplateText = ChatColor.translateAlternateColorCodes('&', getConfigString("nameplate-text", ""));
		if (entityType == EntityType.PLAYER) {
			uuid = getConfigString("uuid", "");
			String skinName = getConfigString("skin", "skin");
			File folder = new File(MagicSpells.getInstance().getDataFolder(), "disguiseskins");
			if (folder.exists()) {
				try {
					File file = new File(folder, skinName + ".skin.txt");
					if (file.exists()) {
						BufferedReader reader = new BufferedReader(new FileReader(file));
						skin = reader.readLine();
						reader.close();
					}
					file = new File(folder, skinName + ".sig.txt");
					if (file.exists()) {
						BufferedReader reader = new BufferedReader(new FileReader(file));
						skinSig = reader.readLine();
						reader.close();
					}
				} catch (Exception e) {
					MagicSpells.handleException(e);
				}
			}
		}
		alwaysShowNameplate = getConfigBoolean("always-show-nameplate", true);
		preventPickups = getConfigBoolean("prevent-pickups", true);
		friendlyMobs = getConfigBoolean("friendly-mobs", true);
		ridingBoat = getConfigBoolean("riding-boat", false);
		undisguiseOnDeath = getConfigBoolean("undisguise-on-death", true);
		undisguiseOnLogout = getConfigBoolean("undisguise-on-logout", false);
		undisguiseOnCast = getConfigBoolean("undisguise-on-cast", false);
		undisguiseOnGiveDamage = getConfigBoolean("undisguise-on-give-damage", false);
		undisguiseOnTakeDamage = getConfigBoolean("undisguise-on-take-damage", false);
		duration = getConfigInt("duration", 0);
		toggle = getConfigBoolean("toggle", false);
		targetSelf = getConfigBoolean("target-self", true);
		strFade = getConfigString("str-fade", "");
				
		if (entityType == null) {
			MagicSpells.error("Invalid entity-type specified for disguise spell '" + spellName + "'");
		}
	}
	
	@Override
	public void initialize() {
		if (manager == null) return;
		super.initialize();
		if (undisguiseOnCast) {
			registerEvents(new CastListener());
		}
		if (undisguiseOnGiveDamage || undisguiseOnTakeDamage) {
			registerEvents(new DamageListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (manager == null) return PostCastAction.ALREADY_HANDLED;
		if (state == SpellCastState.NORMAL) {
			Disguise oldDisguise = disguised.remove(player.getName().toLowerCase());
			manager.removeDisguise(player);
			if (oldDisguise != null && toggle) {
				sendMessage(player, strFade);
				return PostCastAction.ALREADY_HANDLED;
			}
			TargetInfo<Player> target = getTargetPlayer(player, power);
			if (target != null) {
				disguise(target.getTarget());
				sendMessages(player, target.getTarget());
				playSpellEffects(EffectPosition.CASTER, player);
				return PostCastAction.NO_MESSAGES;
			} else {
				return noTarget(player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void disguise(Player player) {
		String nameplate = nameplateText;
		if (showPlayerName) nameplate = player.getDisplayName();
		PlayerDisguiseData playerDisguiseData = (entityType == EntityType.PLAYER ? new PlayerDisguiseData(uuid, skin, skinSig) : null);
		Disguise disguise = new Disguise(player, entityType, nameplate, playerDisguiseData, alwaysShowNameplate, ridingBoat, flag, var1, var2, var3, duration, this);
		manager.addDisguise(player, disguise);
		disguised.put(player.getName().toLowerCase(), disguise);
		playSpellEffects(EffectPosition.TARGET, player);
	}
	
	public void undisguise(Player player) {
		Disguise disguise = disguised.remove(player.getName().toLowerCase());
		if (disguise != null) {
			disguise.cancelDuration();
			sendMessage(player, strFade);
			playSpellEffects(EffectPosition.DISABLED, player);
		}
	}
	
	@Override
	public boolean castAtEntity(Player player, LivingEntity target, float power) {
		if (target instanceof Player) {
			disguise((Player)target);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (target instanceof Player) {
			disguise((Player)target);
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onPickup(PlayerPickupItemEvent event) {
		if (preventPickups && disguised.containsKey(event.getPlayer().getName().toLowerCase())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (undisguiseOnDeath && disguised.containsKey(event.getEntity().getName().toLowerCase())) {
			manager.removeDisguise(event.getEntity(), false);
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (undisguiseOnLogout && disguised.containsKey(event.getPlayer().getName().toLowerCase())) {
			manager.removeDisguise(event.getPlayer(), false);
		}
	}
	
	@EventHandler
	public void onTarget(EntityTargetEvent event) {
		if (friendlyMobs && event.getTarget() != null && event.getTarget() instanceof Player && disguised.containsKey(((Player)event.getTarget()).getName().toLowerCase())) {
			event.setCancelled(true);
		}
	}
	
	class CastListener implements Listener {
		@EventHandler
		void onSpellCast(SpellCastedEvent event) {
			if (event.getSpell() != thisSpell && disguised.containsKey(event.getCaster().getName().toLowerCase())) {
				manager.removeDisguise(event.getCaster());
			}
		}
	}
	
	class DamageListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		void onDamage(EntityDamageEvent event) {
			if (undisguiseOnTakeDamage && event.getEntity() instanceof Player && disguised.containsKey(((Player)event.getEntity()).getName().toLowerCase())) {
				manager.removeDisguise((Player)event.getEntity());
			}
			if (undisguiseOnGiveDamage && event instanceof EntityDamageByEntityEvent) {
				Entity e = ((EntityDamageByEntityEvent)event).getDamager();
				if (e instanceof Player) {
					if (disguised.containsKey(((Player)e).getName().toLowerCase())) {
						manager.removeDisguise((Player)e);
					}
				} else if (e instanceof Projectile && ((Projectile)e).getShooter() instanceof Player) {
					Player shooter = (Player)((Projectile)e).getShooter();
					if (disguised.containsKey(shooter.getName().toLowerCase())) {
						manager.removeDisguise(shooter);
					}
				}
			}
		}
	}
	
	public static DisguiseManager getDisguiseManager() {
		return manager;
	}
	
	@Override
	public void turnOff() {
		if (manager != null) {
			for (String name : new ArrayList<String>(disguised.keySet())) {
				Player player = Bukkit.getPlayerExact(name);
				if (player != null) {
					manager.removeDisguise(player, false);
				}
			}
			manager.unregisterSpell(this);
			if (manager.registeredSpellsCount() == 0) {
				manager.destroy();
				manager = null;
			}
		}
	}

	
	public class Disguise {

		private Player player;
		private EntityType entityType;
		private String nameplateText;
		private PlayerDisguiseData playerDisguiseData;
		private boolean alwaysShowNameplate;
		private boolean ridingBoat;
		private boolean flag;
		private int var1;
		private int var2;
		private int var3;
		private DisguiseSpell spell;
		
		private int taskId;
		
		public Disguise(Player player, EntityType entityType, String nameplateText, PlayerDisguiseData playerDisguiseData, boolean alwaysShowNameplate, boolean ridingBoat, boolean flag, int var1, int var2, int var3, int duration, DisguiseSpell spell) {
			this.player = player;
			this.entityType = entityType;
			this.nameplateText = nameplateText;
			this.playerDisguiseData = playerDisguiseData;
			this.alwaysShowNameplate = alwaysShowNameplate;
			this.ridingBoat = ridingBoat;
			this.flag = flag;
			this.var1 = var1;
			this.var2 = var2;
			this.var3 = var3;
			if (duration > 0) {
				startDuration(duration);
			}
			this.spell = spell;
		}
		
		public Player getPlayer() {
			return player;
		}
		
		public EntityType getEntityType() {
			return entityType;
		}
		
		public String getNameplateText() {
			return nameplateText;
		}
		
		public PlayerDisguiseData getPlayerDisguiseData() {
			return playerDisguiseData;
		}
		
		public boolean alwaysShowNameplate() {
			return alwaysShowNameplate;
		}
		
		public boolean isRidingBoat() {
			return ridingBoat;
		}
		
		public boolean getFlag() {
			return flag;
		}
		
		public int getVar1() {
			return var1;
		}
		
		public int getVar2() {
			return var2;
		}
		
		public int getVar3() {
			return var3;
		}
			
		private void startDuration(int duration) {
			taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					DisguiseSpell.manager.removeDisguise(player);
				}
			}, duration);
		}
		
		public void cancelDuration() {
			if (taskId > 0) {
				Bukkit.getScheduler().cancelTask(taskId);
				taskId = 0;
			}
		}
		
		public DisguiseSpell getSpell() {
			return spell;
		}
		
	}
	
	public class PlayerDisguiseData {
		public String uuid;
		public String skin;
		public String sig;
		public PlayerDisguiseData(String uuid, String skin, String sig) {
			this.uuid = uuid;
			this.skin = skin;
			this.sig = sig;
		}
	}
	
}
