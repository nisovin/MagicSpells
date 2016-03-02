package com.nisovin.magicspells.volatilecode;

import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.material.Button;
import org.bukkit.material.Lever;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;

public class VolatileCodeDisabled implements VolatileCodeHandle {

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
	}

	@Override
	public void entityPathTo(LivingEntity entity, LivingEntity target) {
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
	}
	
	@Override
	public void toggleLeverOrButton(Block block) {
		if (block.getType() == Material.STONE_BUTTON || block.getType() == Material.WOOD_BUTTON) {
			BlockState state = block.getState();
			Button button = (Button)state.getData();
			button.setPowered(true);
			state.update();
		} else if (block.getType() == Material.LEVER) {
			BlockState state = block.getState();
			Lever lever = (Lever)state.getData();
			lever.setPowered(!lever.isPowered());
			state.update();
		}
	}

	@Override
	public void pressPressurePlate(Block block) {
		block.setData((byte) (block.getData() ^ 0x1));
	}

	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
		return false;
	}

	@Override
	public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire, boolean breakBlocks) {
		return location.getWorld().createExplosion(location, size, fire);
	}

	@Override
	public void playExplosionEffect(Location location, float size) {
		location.getWorld().createExplosion(location, 0F);
	}

	@Override
	public void setExperienceBar(Player player, int level, float percent) {
	}

	@Override
	public Fireball shootSmallFireball(Player player) {
		return player.launchProjectile(SmallFireball.class);
	}

	@Override
	public void setTarget(LivingEntity entity, LivingEntity target) {
		if (entity instanceof Creature) {
			((Creature)entity).setTarget(target);
		}
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
	}

	@SuppressWarnings("deprecation")
	@Override
	public void playSound(Player player, String sound, float volume, float pitch) {
		player.playSound(player.getLocation(), sound, volume, pitch);
	}

	@Override
	public ItemStack addFakeEnchantment(ItemStack item) {
		return item;
	}

	@Override
	public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max) {
	}
	
	@Override
	public void playEntityAnimation(Location location, EntityType entityType, int animationId, boolean instant) {
		if (entityType == EntityType.OCELOT && animationId == 7) {
			Ocelot entity = (Ocelot)location.getWorld().spawnEntity(location, entityType);
			entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0));
			entity.playEffect(EntityEffect.WOLF_HEARTS);
			entity.remove();
		}
	}

	@Override
	public void createFireworksExplosion(Location location, boolean flicker, boolean trail, int type, int[] colors, int[] fadeColors, int flightDuration) {
		FireworkEffect.Type t = Type.BALL;
		if (type == 1) {
			t = Type.BALL_LARGE;
		} else if (type == 2) {
			t = Type.STAR;
		} else if (type == 3) {
			t = Type.CREEPER;
		} else if (type == 4) {
			t = Type.BURST;
		}
		Color[] c1 = new Color[colors.length];
		for (int i = 0; i < colors.length; i++) {
			c1[i] = Color.fromRGB(colors[i]);
		}
		Color[] c2 = new Color[fadeColors.length];
		for (int i = 0; i < fadeColors.length; i++) {
			c2[i] = Color.fromRGB(fadeColors[i]);
		}
		FireworkEffect effect = FireworkEffect.builder()
			.flicker(flicker)
			.trail(trail)
			.with(t)
			.withColor(c1)
			.withFade(c2)
			.build();
		Firework firework = location.getWorld().spawn(location, Firework.class);
		FireworkMeta meta = firework.getFireworkMeta();
		meta.addEffect(effect);
		meta.setPower(flightDuration < 1 ? 1 : flightDuration);
		firework.setFireworkMeta(meta);
	}
	
	@Override
	public void playParticleEffect(Location location, String name, float spreadHoriz, float spreadVert, float speed, int count, int radius, float yOffset) {
	}
	
	@Override
	public void playParticleEffect(Location location, String name, float spreadX, float spreadY, float spreadZ, float speed, int count, int radius, float yOffset) {
	}
	
	@Override
	public void playDragonDeathEffect(Location location) {
		
	}
	
	@Override
	public void setKiller(LivingEntity entity, Player killer) {
		
	}

	@Override
	public DisguiseManager getDisguiseManager(MagicConfig config) {
		return null;
	}

	@Override
	public ItemStack addAttributes(ItemStack item, String[] names, String[] types, double[] amounts, int[] operations) {
		return item;
	}

	@Override
	public void removeAI(LivingEntity entity) {
	}
	
	@Override
	public void setNoAIFlag(LivingEntity entity) {
	}

	@Override
	public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation) {
	}

	@Override
	public void resetEntityAttributes(LivingEntity entity) {
	}

	@Override
	public void addAILookAtPlayer(LivingEntity entity, int range) {
	}
	
	@Override
	public void setBossBar(Player player, String title, double percent) {		
	}
	
	@Override
	public void updateBossBar(Player player, String title, double percent) {		
	}
	
	@Override
	public void removeBossBar(Player player) {		
	}
	
	@Override
	public void saveSkinData(Player player, String name) {		
	}

	@Override
	public ItemStack setUnbreakable(ItemStack item) {
		return item;
	}

	@Override
	public void setArrowsStuck(LivingEntity entity, int count) {
	}

	@Override
	public void sendTitleToPlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
	}

	@Override
	public void sendActionBarMessage(Player player, String message) {
	}

	@Override
	public void setTabMenuHeaderFooter(Player player, String header, String footer) {
	}
	
	@Override
	public ItemStack hideTooltipCrap(ItemStack item) {
		return item;
	}
	
	@Override
	public void setClientVelocity(Player player, Vector velocity) {
		player.setVelocity(velocity);
	}
	
	@Override
	public double getAbsorptionHearts(LivingEntity entity) {
		return 0;
	}

	@Override
	public void setOffhand(Player player, ItemStack item) {
	}

	@Override
	public ItemStack getOffhand(Player player) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void showItemCooldown(Player player, ItemStack item, int duration) {
		// TODO Auto-generated method stub
		
	}
}
