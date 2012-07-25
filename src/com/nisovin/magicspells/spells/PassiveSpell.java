package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class PassiveSpell extends Spell {

	private Random random = new Random();
	private boolean disabled = false;
	
	private List<String> triggers;
	private float chance;
	private boolean castWithoutTarget;
	
	private List<String> spellNames;
	private List<Spell> spells;
	
	public PassiveSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		triggers = getConfigStringList("triggers", null);
		chance = getConfigFloat("chance", 100F) / 100F;
		castWithoutTarget = getConfigBoolean("cast-without-target", false);
		
		spellNames = getConfigStringList("spells", null);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		// create spell list
		spells = new ArrayList<Spell>();
		if (spellNames != null) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null) {
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
			for (String trigger : triggers) {
				String type = trigger;
				String var = null;
				if (trigger.contains(" ")) {
					String[] data = trigger.split(" ", 2);
					type = data[0];
					var = data[1];
				}
				type = type.toLowerCase();
				
				// process trigger
				if (type.equalsIgnoreCase("takedamage")) {
					registerEvents(new TakeDamageListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("givedamage")) {
					registerEvents(new GiveDamageListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("kill")) {
					registerEvents(new KillListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("blockbreak")) {
					registerEvents(new BlockBreakListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("blockplace")) {
					registerEvents(new BlockPlaceListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("rightclick")) {
					registerEvents(new RightClickListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("spellcast")) {
					registerEvents(new SpellCastListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("spelltarget")) {
					registerEvents(new SpellTargetListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("spelltargeted")) {
					registerEvents(new SpellTargetedListener(var));
					trigCount++;
				} else if (type.equalsIgnoreCase("sprint")) {
					registerEvents(new SprintListener());
					trigCount++;
				} else if (type.equalsIgnoreCase("sneak")) {
					registerEvents(new SneakListener());
					trigCount++;
				} else if (type.equalsIgnoreCase("stopsprint")) {
					registerEvents(new StopSprintListener());
					trigCount++;
				} else if (type.equalsIgnoreCase("stopsneak")) {
					registerEvents(new StopSneakListener());
					trigCount++;
				} else if (type.equalsIgnoreCase("buff")) {
					registerEvents(new BuffListener());
					trigCount++;
					// fix up the buff spells
					for (Spell spell : spells) {
						if (spell instanceof BuffSpell) {
							BuffSpell buff = (BuffSpell)spell;
							buff.duration = 0;
							buff.numUses = 0;
							buff.useCostInterval = 0;
						}
					}
					// add the buff spells any online players with this spell
					for (Player p : Bukkit.getOnlinePlayers()) {
						if (MagicSpells.getSpellbook(p).hasSpell(this)) {
							for (Spell spell : spells) {
								if (spell instanceof BuffSpell) {
									spell.castSpell(p, SpellCastState.NORMAL, 1.0F, null);
								}
							}
						}
					}
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
	
	private boolean hasSpell(Player caster) {
		return MagicSpells.getSpellbook(caster).hasSpell(this);
	}
	
	private void activate(Player caster) {
		activate(caster, null, null);
	}
	
	private void activate(Player caster, LivingEntity target) {
		activate(caster, target, null);
	}
	
	private void activate(Player caster, Location location) {
		activate(caster, null, location);
	}
	
	private void activate(Player caster, LivingEntity target, Location location) {
		MagicSpells.debug(3, "Activating passive spell '" + name + "' for player " + caster.getName());
		if (!disabled && (chance >= .999 || random.nextFloat() <= chance) && getCastState(caster) == SpellCastState.NORMAL) {
			disabled = true;
			SpellCastEvent event = new SpellCastEvent(this, caster, SpellCastState.NORMAL, 1.0F, null, this.cooldown, this.reagents.clone(), 0);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				if (event.haveReagentsChanged() && !hasReagents(caster, event.getReagents())) {
					return;
				}
				setCooldown(caster, event.getCooldown());
				float power = event.getPower();
				for (Spell spell : spells) {
					MagicSpells.debug(3, "    Casting spell effect '" + spell.getName() + "'");
					if (castWithoutTarget || !(spell instanceof TargetedSpell)) {
						spell.castSpell(caster, SpellCastState.NORMAL, power, null);
						playSpellEffects(EffectPosition.CASTER, caster);
					} else if (spell instanceof TargetedEntitySpell && target != null) {
						((TargetedEntitySpell)spell).castAtEntity(caster, target, power);
						playSpellEffects(caster, target);
					} else if (spell instanceof TargetedLocationSpell && (location != null || target != null)) {
						if (location != null) {
							((TargetedLocationSpell)spell).castAtLocation(caster, location, power);
							playSpellEffects(caster, location);
						} else if (target != null) {
							((TargetedLocationSpell)spell).castAtLocation(caster, target.getLocation(), power);
							playSpellEffects(caster, target.getLocation());
						}
					}
				}
				removeReagents(caster, event.getReagents());
				sendMessage(caster, strCastSelf);				
			}
			disabled = false;
		}
	}
	
	public class TakeDamageListener implements Listener {
		
		int[] itemIds = null;
		DamageCause[] damageCauses = null;
		
		public TakeDamageListener(String var) {
			if (var != null) {
				var = var.replace(", ", ",");
				if (var.matches("[0-9]+(,[0-9]+)*")) {
					String[] s = var.split(",");
					itemIds = new int[s.length];
					for (int i = 0; i < s.length; i++) {
						itemIds[i] = Integer.parseInt(s[i]);
					}
				} else {
					String[] s = var.split(",");
					damageCauses = new DamageCause[s.length];
					for (int i = 0; i < s.length; i++) {
						damageCauses[i] = DamageCause.valueOf(s[i].toUpperCase());
					}
				}
			}
		}
		
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onDamage(EntityDamageEvent event) {
			if (event.getEntityType() == EntityType.PLAYER && ((Player)event.getEntity()).getNoDamageTicks() == 0) {
				Player player = (Player)event.getEntity();
				if (hasSpell(player)) {
					DamageCause cause = event.getCause();
					if (event instanceof EntityDamageByEntityEvent) {
						Entity attacker = ((EntityDamageByEntityEvent)event).getDamager();
						if (attacker instanceof Projectile) {
							attacker = ((Projectile)attacker).getShooter();
							if (check(attacker, cause)) activate(player, (LivingEntity)attacker);
						} else if (attacker instanceof LivingEntity) {
							if (check(attacker, cause)) activate(player, (LivingEntity)attacker);
						} else {
							if (check(attacker, cause)) activate(player);
						}
					} else {
						if (check(null, cause)) activate(player);
					}
				}
			}
		}
		
		private boolean check(Entity attacker, DamageCause cause) {
			if (itemIds != null && attacker != null) {
				if (attacker instanceof Player) {
					ItemStack inHand = ((Player)attacker).getItemInHand();
					if (inHand != null && Util.arrayContains(itemIds, inHand.getTypeId())) {
						return true;
					} else if (inHand == null && Util.arrayContains(itemIds, 0)) {
						return true;
					}
				} else if (attacker instanceof Skeleton && Util.arrayContains(itemIds, Material.BOW.getId())) {
					return true;
				} else if (attacker instanceof Zombie && Util.arrayContains(itemIds, 0)) {
					return true;
				}
				return false;
			} else if (damageCauses != null) {
				return Util.arrayContains(damageCauses, cause);
			} else {
				return true;
			}
		}
	}
	
	public class GiveDamageListener implements Listener {
		
		int[] itemIds = null;
		
		public GiveDamageListener(String var) {
			if (var != null) {
				var = var.replace(" ", "");
				if (var.matches("[0-9]+(,[0-9]+)*")) {
					String[] s = var.split(",");
					itemIds = new int[s.length];
					for (int i = 0; i < s.length; i++) {
						itemIds[i] = Integer.parseInt(s[i]);
					}
				}
			}
		}
		
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onDamage(EntityDamageByEntityEvent event) {
			if (event.getDamage() == 0) return;
			if (event.getEntity() instanceof LivingEntity && (((LivingEntity)event.getEntity()).getHealth() <= 0 || ((LivingEntity)event.getEntity()).getNoDamageTicks() > 0)) return;
			Player player = null;
			if (event.getDamager().getType() == EntityType.PLAYER) {
				player = (Player)event.getDamager();
			} else if (event.getDamager() instanceof Projectile) {
				LivingEntity shooter = ((Projectile)event.getDamager()).getShooter();
				if (shooter != null && shooter.getType() == EntityType.PLAYER) {
					player = (Player)((Projectile)event.getDamager()).getShooter();
				}
			}
			if (player != null) {
				if (itemIds != null) {
					int id = 0;
					if (player.getItemInHand() != null) id = player.getItemInHand().getTypeId();
					if (!Util.arrayContains(itemIds, id)) {
						return;
					}
				}
				if (hasSpell(player)) {
					activate(player, (LivingEntity)event.getEntity());
				}
			}
		}
	}
	
	public class KillListener implements Listener {
		
		EntityType[] types = null;
		
		public KillListener(String var) {
			if (var != null) {
				var = var.replace(" ", "");
				String[] s = var.split(",");
				types = new EntityType[s.length];
				for (int i = 0; i < s.length; i++) {
					types[i] = EntityType.fromName(s[i].toUpperCase());
				}
			}
		}
		
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onDeath(EntityDeathEvent event) {
			Player killer = event.getEntity().getKiller();
			if (killer != null && (types == null || Util.arrayContains(types, event.getEntity().getType())) && hasSpell(killer)) {
				activate(killer);
			}
		}
		
	}
	
	public class BlockBreakListener implements Listener {
		
		int[] typeIds = null;
		
		public BlockBreakListener(String var) {
			if (var != null) {
				var = var.replace(" ", "");
				if (var.matches("[0-9]+(,[0-9]+)*")) {
					String[] s = var.split(",");
					typeIds = new int[s.length];
					for (int i = 0; i < s.length; i++) {
						typeIds[i] = Integer.parseInt(s[i]);
					}
				}
			}
		}
		
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onBlockBreak(BlockBreakEvent event) {
			if (typeIds == null || Util.arrayContains(typeIds, event.getBlock().getTypeId())) {
				Player player = event.getPlayer();
				if (hasSpell(player)) {
					activate(player, event.getBlock().getLocation().add(.5, 0, .5));
				}
			}
		}
	}
	
	public class BlockPlaceListener implements Listener {
		
		int[] typeIds = null;
		
		public BlockPlaceListener(String var) {
			if (var != null) {
				var = var.replace(" ", "");
				if (var.matches("[0-9]+(,[0-9]+)*")) {
					String[] s = var.split(",");
					typeIds = new int[s.length];
					for (int i = 0; i < s.length; i++) {
						typeIds[i] = Integer.parseInt(s[i]);
					}
				}
			}
		}
		
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onBlockPlace(BlockPlaceEvent event) {
			if (typeIds == null || Util.arrayContains(typeIds, event.getBlock().getTypeId())) {
				Player player = event.getPlayer();
				if (hasSpell(player)) {
					activate(player, event.getBlock().getLocation().add(.5, 0, .5));
				}
			}
		}
	}
	
	public class RightClickListener implements Listener {
		int typeIds[] = new int[0];
		int datas[] = new int[0];
		boolean checkData[] = new boolean[0];
		
		public RightClickListener(String var) {
			if (var != null) {
				var = var.replace(" ", "");
				if (var != null && var.matches("[0-9]+(:[0-9]+)?(,[0-9]+(:[0-9]+)?)*")) {
					String[] vars = var.split(",");
					typeIds = new int[vars.length];
					datas = new int[vars.length];
					checkData = new boolean[vars.length];
					for (int i = 0; i < vars.length; i++) {
						if (vars[i].contains(":")) {
							String[] s = vars[i].split(":");
							typeIds[i] = Integer.parseInt(s[0]);
							datas[i] = Integer.parseInt(s[1]);
							checkData[i] = true;
						} else {
							typeIds[i] = Integer.parseInt(vars[i]);
							datas[i] = 0;
							checkData[i] = false;
						}
					}
				}
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerInteract(PlayerInteractEvent event) {
			if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.hasItem() && event.useItemInHand() != Result.DENY) {
				if (checkItem(event.getItem())) {
					Player player = event.getPlayer();
					if (hasSpell(player)) {
						activate(player);
					}
				}
			}
		}
		
		private boolean checkItem(ItemStack item) {
			for (int i = 0; i < typeIds.length; i++) {
				if (item.getTypeId() == typeIds[i] && (!checkData[i] || item.getDurability() == datas[i])) {
					return true;
				}
			}
			return false;
		}
	}
	
	public class SpellTargetedListener implements Listener {

		String spellNames[] = null;
		
		public SpellTargetedListener(String var) {
			if (var != null) {
				spellNames = var.split(",");
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellTarget(SpellTargetEvent event) {
			if (event.getTarget() instanceof Player) {
				if (spellNames == null || Util.arrayContains(spellNames, event.getSpell().getInternalName())) {
					Player player = (Player)event.getTarget();
					if (hasSpell(player)) {
						activate(player, event.getCaster());
					}
				}
			}
		}
	}
	
	public class SpellCastListener implements Listener {
		
		String spellNames[] = null;
		
		public SpellCastListener(String var) {
			if (var != null) {
				spellNames = var.split(",");
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR)
		public void onSpellCasted(SpellCastedEvent event) {
			if ((spellNames == null || Util.arrayContains(spellNames, event.getSpell().getInternalName())) && event.getPostCastAction() != PostCastAction.ALREADY_HANDLED && event.getSpellCastState() == SpellCastState.NORMAL) {
				if (hasSpell(event.getCaster())) {
					activate(event.getCaster());
				}
			}
		}
	}
	
	public class SpellTargetListener implements Listener {
		
		String spellNames[] = null;
		
		public SpellTargetListener(String var) {
			if (var != null) {
				spellNames = var.split(",");
			}
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellTarget(SpellTargetEvent event) {
			if (spellNames == null || Util.arrayContains(spellNames, event.getSpell().getInternalName())) {
				if (hasSpell(event.getCaster())) {
					activate(event.getCaster(), event.getTarget());
				}
			}
		}
	}
	
	public class SprintListener implements Listener {
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onSprint(PlayerToggleSprintEvent event) {
			if (event.isSprinting() && hasSpell(event.getPlayer())) {
				activate(event.getPlayer());
			}
		}
	}
	
	public class SneakListener implements Listener {
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onSneak(PlayerToggleSneakEvent event) {
			if (event.isSneaking() && hasSpell(event.getPlayer())) {
				activate(event.getPlayer());
			}
		}
	}
	
	public class StopSprintListener implements Listener {
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onSprint(PlayerToggleSprintEvent event) {
			if (!event.isSprinting() && hasSpell(event.getPlayer())) {
				activate(event.getPlayer());
			}
		}
	}
	
	public class StopSneakListener implements Listener {
		@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
		public void onSneak(PlayerToggleSneakEvent event) {
			if (!event.isSneaking() && hasSpell(event.getPlayer())) {
				activate(event.getPlayer());
			}
		}
	}
	
	public class BuffListener implements Listener {
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent event) {
			on(event.getPlayer());
		}
		
		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			off(event.getPlayer());
		}
		
		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent event) {
			off(event.getEntity());
		}
		
		@EventHandler
		public void onPlayerRespawn(final PlayerRespawnEvent event) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					on(event.getPlayer());
				}
			}, 1);
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellLearn(final SpellLearnEvent event) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					on(event.getLearner());
				}
			}, 1);
		}
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellForget(SpellForgetEvent event) {
			off(event.getForgetter());
		}
		
		private void on(Player player) {
			if (hasSpell(player)) {
				for (Spell spell : spells) {
					if (spell instanceof BuffSpell) {
						BuffSpell buff = (BuffSpell)spell;
						if (!buff.isActive(player)) {
							buff.castSpell(player, SpellCastState.NORMAL, 1.0F, null);
						}
					}
				}
			}
		}
		
		private void off(Player player) {
			if (hasSpell(player)) {
				for (Spell spell : spells) {
					if (spell instanceof BuffSpell) {
						BuffSpell buff = (BuffSpell)spell;
						if (buff.isActive(player)) {
							buff.turnOff(player);
						}
					}
				}
			}
		}
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

}
