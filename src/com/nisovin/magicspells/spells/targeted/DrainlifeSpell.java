package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class DrainlifeSpell extends TargetedEntitySpell {
	
	private String takeType;
	private int takeAmt;
	private String giveType;
	private int giveAmt;
	private boolean showSpellEffect;
	private int animationSpeed;
	private boolean ignoreArmor;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	private String strNoTarget;
	
	public DrainlifeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		takeType = getConfigString("take-type", "health");
		takeAmt = getConfigInt("take-amt", 2);
		giveType = getConfigString("give-type", "health");
		giveAmt = getConfigInt("give-amt", 2);
		showSpellEffect = getConfigBoolean("show-spell-effect", true);
		animationSpeed = getConfigInt("animation-speed", 2);
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		strNoTarget = getConfigString("str-no-target", "");
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail: no target
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				boolean drained = drain(player, target, power);
				if (!drained) {
					sendMessage(player, strNoTarget);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean drain(Player player, LivingEntity target, float power) {
		int take = Math.round(takeAmt*power);
		int give = Math.round(giveAmt*power);
		
		// drain from target
		if (takeType.equals("health")) {
			if (target instanceof Player && checkPlugins) {
				EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, take);
				Bukkit.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					return false;
				}
				take = event.getDamage();
			}
			if (ignoreArmor) {
				int health = target.getHealth() - take;
				if (health < 0) health = 0;
				target.setHealth(health);
			} else {
				target.damage(take);
			}
		} else if (takeType.equals("mana")) {
			if (target instanceof Player) {
				boolean removed = MagicSpells.getManaHandler().removeMana((Player)target, take);
				if (!removed) {
					give = 0;
				}
			}
		} else if (takeType.equals("hunger")) {
			if (target instanceof Player) {
				Player p = (Player)target;
				int food = p.getFoodLevel();
				if (give > food) give = food;
				food -= take;
				if (food < 0) food = 0;
				p.setFoodLevel(food);
			}
		} else if (takeType.equals("experience")) {
			if (target instanceof Player) {
				Player p = (Player)target;
				int exp = p.getTotalExperience();
				if (give > exp) give = exp;
				exp -= take;
				if (exp < 0) exp = 0;
				p.setTotalExperience(exp);
			}
		}
		
		// give to caster
		if (giveType.equals("health")) {
			int h = player.getHealth()+Math.round(give);
			if (h>20) h=20;
			player.setHealth(h);
		} else if (giveType.equals("mana")) {
			MagicSpells.getManaHandler().addMana(player, give);
		} else if (takeType.equals("hunger")) {
			int food = player.getFoodLevel();
			food += take;
			if (food > 20) food = 20;
			player.setFoodLevel(food);
		} else if (takeType.equals("experience")) {
			int exp = player.getTotalExperience();
			exp += take;
			player.setTotalExperience(exp);
		}
		
		// show animation
		if (showSpellEffect) {
			new DrainlifeAnimation(player, target);
		}
		
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player && !targetPlayers) {
			return false;
		} else {
			return drain(caster, target, power);
		}
	}
	
	private class DrainlifeAnimation implements Runnable {
		
		private int taskId;
		private int i;
		private ArrayList<Block> blocks;
		private World world;
		
		public DrainlifeAnimation(Player player, LivingEntity target) {			
			// get blocks to animate
			Vector start = target.getLocation().toVector();
			Vector playerVector = player.getLocation().toVector();
			double distanceSq = start.distanceSquared(playerVector);
			Vector direction = playerVector.subtract(start);
			BlockIterator iterator = new BlockIterator(player.getWorld(), start, direction, player.getEyeHeight(), range);
			blocks = new ArrayList<Block>();
			Block b;
			while (iterator.hasNext()) {
				b = iterator.next();
				if (b != null && b.getType() == Material.AIR) {
					blocks.add(b);
				} else {
					break;
				}
				if (b.getLocation().toVector().distanceSquared(start) > distanceSq) {
					break;
				}
			}
			
			// start animation
			world = player.getWorld();
			if (blocks.size() > 0) {
				i = 0;
				taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, animationSpeed, animationSpeed);
			}
		}

		@Override
		public void run() {
			if (blocks.size() > i) {
				Block b = blocks.get(i);
				world.playEffect(b.getLocation(), Effect.SMOKE, 4);
				i++;
			} else {
				Bukkit.getServer().getScheduler().cancelTask(taskId);
			}
		}
		
	}

}
