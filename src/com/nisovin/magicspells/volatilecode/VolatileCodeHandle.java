package com.nisovin.magicspells.volatilecode;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;

public interface VolatileCodeHandle {
	
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration);
	
	public void entityPathTo(LivingEntity entity, LivingEntity target);
	
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item);
	
	public void toggleLeverOrButton(Block block);
	
	public void pressPressurePlate(Block block);
	
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire);
	
	public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire, boolean breakBlocks);
	
	public void playExplosionEffect(Location location, float size);
	
	public void setExperienceBar(Player player, int level, float percent);
	
	public Fireball shootSmallFireball(Player player);
	
	public void setTarget(LivingEntity entity, LivingEntity target);
	
	public void playSound(Location location, String sound, float volume, float pitch);
	
	public void playSound(Player player, String sound, float volume, float pitch);
	
	public ItemStack addFakeEnchantment(ItemStack item);
	
	public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max);
	
	//public void addPotionEffect(LivingEntity entity, PotionEffect effect, boolean ambient);
	
	public void playEntityAnimation(Location location, EntityType entityType, int animationId, boolean instant);
	
	public void createFireworksExplosion(Location location, boolean flicker, boolean trail, int type, int[] colors, int[] fadeColors, int flightDuration);
	
	//public void setHeldItemSlot(Player player, int slot);
	
	public void playParticleEffect(Location location, String name, float spreadHoriz, float spreadVert, float speed, int count, int radius, float yOffset);
	
	public void playParticleEffect(Location location, String name, float spreadX, float spreadY, float spreadZ, float speed, int count, int radius, float yOffset);
	
	public void setKiller(LivingEntity entity, Player killer);
	
	public DisguiseManager getDisguiseManager(MagicConfig config);
	
	public void playDragonDeathEffect(Location location);
	
	public ItemStack addAttributes(ItemStack item, String[] names, String[] types, double[] amounts, int[] operations);
	
	public ItemStack hideTooltipCrap(ItemStack item);
	
	public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation);
	
	public void resetEntityAttributes(LivingEntity entity);
	
	public void removeAI(LivingEntity entity);
	
	public void setNoAIFlag(LivingEntity entity);
	
	public void addAILookAtPlayer(LivingEntity entity, int range);
	
	public void setBossBar(Player player, String title, double percent);
	
	public void updateBossBar(Player player, String title, double percent);
	
	public void removeBossBar(Player player);
	
	public void saveSkinData(Player player, String name);
	
	public ItemStack setUnbreakable(ItemStack item);
	
	public void setArrowsStuck(LivingEntity entity, int count);
	
	public void sendTitleToPlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);
	
	public void sendActionBarMessage(Player player, String message);
	
	public void setTabMenuHeaderFooter(Player player, String header, String footer);
	
	public void setClientVelocity(Player player, Vector velocity);
	
	public double getAbsorptionHearts(LivingEntity entity);
	
	public void setOffhand(Player player, ItemStack item);
	
	public ItemStack getOffhand(Player player);
	
	public void showItemCooldown(Player player, ItemStack item, int duration);
	
}
