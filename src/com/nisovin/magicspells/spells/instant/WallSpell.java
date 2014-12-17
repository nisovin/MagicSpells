package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TemporaryBlockSet;
import com.nisovin.magicspells.util.TemporaryBlockSet.BlockSetRemovalCallback;

public class WallSpell extends InstantSpell {

	private int distance;
	private int wallWidth;
	private int wallHeight;
	private int wallDepth;
	private MagicMaterial wallMaterial;
	private int wallDuration;
	private boolean preventBreaking;
	private boolean preventDrops;
	private boolean checkPlugins;
	private String strNoTarget;
	
	private ArrayList<TemporaryBlockSet> blockSets;
	
	public WallSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		distance = getConfigInt("distance", 3);
		wallWidth = getConfigInt("wall-width", 5);
		wallHeight = getConfigInt("wall-height", 3);
		wallDepth = getConfigInt("wall-depth", 1);
		String type = getConfigString("wall-type", "stone");
		wallMaterial = MagicSpells.getItemNameResolver().resolveBlock(type);
		wallDuration = getConfigInt("wall-duration", 15);
		preventBreaking = getConfigBoolean("prevent-breaking", false);
		preventDrops = getConfigBoolean("prevent-drops", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		strNoTarget = getConfigString("str-no-target", "Unable to create a wall.");
		
		blockSets = new ArrayList<TemporaryBlockSet>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if ((preventBreaking || preventDrops) && wallDuration > 0) {
			registerEvents(new BreakListener());
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = getTargetedBlock(player, (distance > 0 && distance < 15) ? distance : 3);
			if (target == null || target.getType() != Material.AIR) {
				// fail
				sendMessage(player, strNoTarget);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				
				// check plugins
				if (checkPlugins) {
					BlockState eventBlockState = target.getState();
					wallMaterial.setBlock(target, false);
					BlockPlaceEvent event = new BlockPlaceEvent(target, eventBlockState, target, player.getItemInHand(), player, true);
					Bukkit.getPluginManager().callEvent(event);
					BlockUtils.setTypeAndData(target, Material.AIR, (byte)0, false);
					if (event.isCancelled()) {
						sendMessage(player, strNoTarget);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
				
				TemporaryBlockSet blockSet = new TemporaryBlockSet(Material.AIR, wallMaterial);
				Location loc = target.getLocation();
				Vector dir = player.getLocation().getDirection();
				int wallWidth = Math.round(this.wallWidth*power);
				int wallHeight = Math.round(this.wallHeight*power);
				if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
					int depthDir = dir.getX() > 0 ? 1 : -1;
					for (int z = loc.getBlockZ() - (wallWidth/2); z <= loc.getBlockZ() + (wallWidth/2); z++) {
						for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + wallHeight - 1; y++) {
							for (int x = target.getX(); x < target.getX() + wallDepth && x > target.getX() - wallDepth; x += depthDir) {
								blockSet.add(player.getWorld().getBlockAt(x, y, z));
							}
						}
					}
				} else {
					int depthDir = dir.getZ() > 0 ? 1 : -1;
					for (int x = loc.getBlockX() - (wallWidth/2); x <= loc.getBlockX() + (wallWidth/2); x++) {
						for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + wallHeight - 1; y++) {
							for (int z = target.getZ(); z < target.getZ() + wallDepth && z > target.getZ() - wallDepth; z += depthDir) {
								blockSet.add(player.getWorld().getBlockAt(x, y, z));
							}
						}
					}
				}
				if (wallDuration > 0) {
					blockSets.add(blockSet);
					blockSet.removeAfter(Math.round(wallDuration*power), new BlockSetRemovalCallback() {
						@Override
						public void run(TemporaryBlockSet set) {
							blockSets.remove(set);
						}
					});
				}
				
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	class BreakListener implements Listener {
		@EventHandler(ignoreCancelled=true)
		void onBlockBreak(BlockBreakEvent event) {
			if (blockSets.size() > 0) {
				for (TemporaryBlockSet blockSet : blockSets) {
					if (blockSet.contains(event.getBlock())) {
						event.setCancelled(true);
						if (!preventBreaking) {
							event.getBlock().setType(Material.AIR);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void turnOff() {
		for (TemporaryBlockSet blockSet : blockSets) {
			blockSet.remove();
		}
		blockSets.clear();
	}
}