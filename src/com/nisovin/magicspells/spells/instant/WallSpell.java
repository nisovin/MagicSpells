package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TemporaryBlockSet;
import com.nisovin.magicspells.util.TemporaryBlockSet.BlockSetRemovalCallback;

public class WallSpell extends InstantSpell {

	private int distance;
	private int wallWidth;
	private int wallHeight;
	private Material wallType;
	private int wallDuration;
	private boolean preventBreaking;
	private String strNoTarget;
	
	private ArrayList<TemporaryBlockSet> blockSets;
	
	public WallSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		distance = getConfigInt("distance", 3);
		wallWidth = getConfigInt("wall-width", 5);
		wallHeight = getConfigInt("wall-height", 3);
		wallType = Material.getMaterial(getConfigInt("wall-type", Material.BRICK.getId()));
		wallDuration = getConfigInt("wall-duration", 15);
		preventBreaking = getConfigBoolean("prevent-breaking", true);
		strNoTarget = getConfigString("str-no-target", "Unable to create a wall.");
		
		if (preventBreaking) {
			blockSets = new ArrayList<TemporaryBlockSet>();
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = player.getTargetBlock(null, distance>0&&distance<15?distance:3);
			if (target == null || target.getType() != Material.AIR) {
				// fail
				sendMessage(player, strNoTarget);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				TemporaryBlockSet blockSet = new TemporaryBlockSet(Material.AIR, wallType);
				Location loc = target.getLocation();
				Vector dir = player.getLocation().getDirection();
				int wallWidth = Math.round(this.wallWidth*power);
				int wallHeight = Math.round(this.wallHeight*power);
				if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
					for (int z = loc.getBlockZ() - (wallWidth/2); z <= loc.getBlockZ() + (wallWidth/2); z++) {
						for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + wallHeight - 1; y++) {
							blockSet.add(player.getWorld().getBlockAt(target.getX(), y, z));
						}
					}
				} else {
					for (int x = loc.getBlockX() - (wallWidth/2); x <= loc.getBlockX() + (wallWidth/2); x++) {
						for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + wallHeight - 1; y++) {
							blockSet.add(player.getWorld().getBlockAt(x, y, target.getZ()));
						}
					}
				}
				if (preventBreaking) {
					blockSets.add(blockSet);
					blockSet.removeAfter(Math.round(wallDuration*power), new BlockSetRemovalCallback() {
						@Override
						public void run(TemporaryBlockSet set) {
							blockSets.remove(set);
						}
					});
				} else {
					blockSet.removeAfter(Math.round(wallDuration*power));
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
}