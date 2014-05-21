package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.BlockChangeDelegate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;

public class TreeSpell extends TargetedSpell implements TargetedLocationSpell {

	private TreeType treeType;
	private int speed;
	
	public TreeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		treeType = TreeType.valueOf(getConfigString("tree-type", "tree").toUpperCase().replace(" ", "_"));
		if (treeType == null) treeType = TreeType.TREE;
		speed = getConfigInt("animation-speed", 20);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target block
			Block target = getTargetedBlock(player, power);

			if (target != null && target.getType() != Material.AIR) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, target.getLocation(), power);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					target = null;
				} else {
					target = event.getTargetLocation().getBlock();
				}
			}
			
			if (target == null || target.getType() == Material.AIR) {
				return noTarget(player);
			}
			
			// grow tree
			boolean grown = growTree(target);
			
			// check if failed
			if (!grown) {
				return noTarget(player);
			} else {
				playSpellEffects(player, target.getLocation());
			}
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private boolean growTree(Block target) {
		// switch to block above
		target = target.getRelative(BlockFace.UP);
		if (target.getType() != Material.AIR) {
			return false;
		}
		
		// grow tree
		Location loc = target.getLocation();				
		if (speed > 0) {
			List<BlockState> blockStates = new ArrayList<BlockState>();
			target.getWorld().generateTree(loc, treeType, new TreeWatch(loc, blockStates));
			if (blockStates.size() > 0) {
				new GrowAnimation(loc.getBlockX(), loc.getBlockZ(), blockStates, speed);
				return true;
			} else {
				return false;
			}
		} else {
			return target.getWorld().generateTree(loc, treeType);
		}
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		boolean ret = growTree(target.getBlock());
		if (ret) {
			playSpellEffects(caster, target);
		}
		return ret;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}
	
	private class GrowAnimation extends SpellAnimation {
		
		List<BlockState> blockStates;
		int blocksPerTick;
		
		public GrowAnimation(final int centerX, final int centerZ, final List<BlockState> blocks, int speed) {
			super(speed < 20 ? 20 / speed : 1, true);
			
			this.blockStates = blocks;
			this.blocksPerTick = speed/20 + 1;
			Collections.sort(blockStates, new Comparator<BlockState>() {
				@Override
				public int compare(BlockState o1, BlockState o2) {				
					if (o1.getY() < o2.getY()) {
						return -1;
					} else if (o1.getY() > o2.getY()) {
						return 1;
					} else {
						int dist1 = Math.abs(o1.getX() - centerX) + Math.abs(o1.getZ() - centerZ);
						int dist2 = Math.abs(o2.getX() - centerX) + Math.abs(o2.getZ() - centerZ);
						if (dist1 > dist2) {
							return 1;
						} else if (dist1 < dist2) {
							return -1;
						} else {
							return 0;
						}
					}
				}
			});
		}

		@Override
		protected void onTick(int tick) {
			for (int i = 0; i < blocksPerTick; i++) {
				BlockState state = blockStates.remove(0);
				state.update(true);
				if (blockStates.size() == 0) {
					stop();
					break;
				}
			}
		}
		
	}
	
	private class TreeWatch implements BlockChangeDelegate {

		private Location loc;
		private List<BlockState> blockStates;
		
		public TreeWatch(Location loc, List<BlockState> blockStates) {
			this.loc = loc;
			this.blockStates = blockStates;
		}
		
		@Override
		public int getHeight() {
			return loc.getWorld().getMaxHeight();
		}

		@Override
		public int getTypeId(int x, int y, int z) {
			return loc.getWorld().getBlockTypeIdAt(x, y, z);
		}

		@Override
		public boolean isEmpty(int x, int y, int z) {
			return getTypeId(x,y,z) == 0;
		}

		@Override
		public boolean setRawTypeId(int x, int y, int z, int id) {
			BlockState state = loc.getWorld().getBlockAt(x, y, z).getState();
			state.setTypeId(id);
			blockStates.add(state);
			return true;
		}

		@Override
		public boolean setRawTypeIdAndData(int x, int y, int z, int id, int data) {
			BlockState state = loc.getWorld().getBlockAt(x, y, z).getState();
			state.setTypeId(id);
			state.setRawData((byte)data);
			blockStates.add(state);
			return true;
		}

		@Override
		public boolean setTypeId(int x, int y, int z, int id) {
			return setRawTypeId(x, y, z, id);
		}

		@Override
		public boolean setTypeIdAndData(int x, int y, int z, int id, int data) {
			return setRawTypeIdAndData(x, y, z, id, data);
		}
		
	}

}
