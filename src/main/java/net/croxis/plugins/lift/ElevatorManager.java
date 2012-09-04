package net.croxis.plugins.lift;

import java.util.HashSet;

import net.h31ix.anticheat.api.AnticheatAPI;
import net.h31ix.anticheat.manage.CheckType;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;

public class ElevatorManager {
	private static Lift plugin;
	public static HashSet<Elevator> lifts = new HashSet<Elevator>();

	public ElevatorManager(Lift plugin) {
		ElevatorManager.plugin = plugin;
	}
	
	public static void createLift(Block block){
		long startTime = System.currentTimeMillis();
		plugin.logDebug("Starting elevator gen");
		Elevator elevator = new Elevator();
		int yscan = block.getY() - 1;
		while(yscan >= 0){
			if (yscan == 0){ //Gone too far with no base abort!
				plugin.logDebug("yscan was 0");
				return;
			}
			Block checkBlock = block.getWorld().getBlockAt(block.getX(), yscan, block.getZ());
			if (isValidShaftBlock(checkBlock)){
				// Do nothing keep going
			} else if (ElevatorManager.isBaseBlock(checkBlock)) {
				scanFloorBlocks(checkBlock, elevator);
				elevator.baseBlockType = block.getType();
				elevator.speed = plugin.blockSpeeds.get(elevator.baseBlockType);
				for (Block b : elevator.floorBlocks){
					// This is for speed optimization for entering lift in use
					if (!elevator.chunks.contains(block.getChunk()))
						elevator.chunks.add(block.getChunk());
				}
				break;
			} else {
				// Something is obstructing the elevator so stop
				if (plugin.debug){
					System.out.println("==Unknown Error==");
					System.out.println("Yscan: " + Integer.toString(yscan));
					System.out.println("Block: " + checkBlock.getType().toString());
					System.out.println("Is Valid Block: " + Boolean.toString(isValidBlock(checkBlock)));
					System.out.println("Is Base Block: " + Boolean.toString(ElevatorManager.isBaseBlock(checkBlock)));
				}
				return;
			}
			yscan--;
		}
		plugin.logDebug("Base size: " + Integer.toString(elevator.floorBlocks.size()));
		
		for (Block b : floorBlocks){
			int x = b.getX();
			int z = b.getZ();
			int y1 = b.getY();
			
			yscan = b.getY();
			World currentWorld = b.getWorld();	
			while (true){
				y1 = y1 + 1;
				Block testBlock = b.getWorld().getBlockAt(x, y1, z);
				if (!isValidBlock(testBlock))
					break;
				if (testBlock.getType() == Material.STONE_BUTTON){
					if (plugin.checkGlass)
						if (!scanGlassAtY(currentWorld, testBlock.getY() - 2))
							break;
					Floor floor = new Floor();
					floor.setY(y1);
					if (testBlock.getRelative(BlockFace.DOWN).getType() == Material.WALL_SIGN)
						floor.setName(((Sign) testBlock.getRelative(BlockFace.DOWN).getState()).getLine(1));
					if (testBlock.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
						floormap.put(y1, floor);
					if (plugin.debug)
						System.out.println("Floor added: " + b.getLocation());
					
				}
			}
		}
		int floorNumber = 1;
		for (Floor floor : floormap.values()){
			floor.setFloor(floorNumber);
			floormap2.put(floorNumber, floor);
			floorNumber = floorNumber + 1;
		}
		
		
		//Elevator is constructed, pass off to check signs for floor destination, collect all people and move them
		if (plugin.debug)
			System.out.println("Elevator gen took: " + (System.currentTimeMillis() - startTime) + " ms.");
	}
	
	//Checks if block is a valid elevator block SANS iron
	public static boolean isValidShaftBlock(Block checkBlock){
		if (checkBlock.getType() == Material.AIR || checkBlock.getType() == plugin.floorBlock
				|| checkBlock.getType() == Material.TORCH || checkBlock.getType() == Material.WALL_SIGN
				|| checkBlock.getType() == Material.STONE_BUTTON || checkBlock.getType() == Material.VINE 
				|| checkBlock.getType() == Material.LADDER || checkBlock.getType() == Material.WATER
				|| checkBlock.getType() == Material.STATIONARY_WATER)
			return true;
		return false;
	}
	
	//Recursive function that constructs our list of blocks
	public static void scanBaseBlocks(Block block, Elevator elevator){
		if (elevator.floorBlocks.size() >= plugin.liftArea)
			return; //5x5 max, prevents infinite loops
		else if (elevator.floorBlocks.contains(block))
			return; // We have that block already
		elevator.floorBlocks.add(block);
		if (isBaseBlock(block.getRelative(BlockFace.NORTH, 1)))
			scanBaseBlocks(block.getRelative(BlockFace.NORTH), elevator);
		if (isBaseBlock(block.getRelative(BlockFace.EAST, 1)))
			scanBaseBlocks(block.getRelative(BlockFace.EAST), elevator);
		if (isBaseBlock(block.getRelative(BlockFace.SOUTH, 1)))
			scanBaseBlocks(block.getRelative(BlockFace.SOUTH), elevator);
		if (isBaseBlock(block.getRelative(BlockFace.WEST, 1)))
			scanBaseBlocks(block.getRelative(BlockFace.WEST), elevator);
		return;
	}
	
	public static void endLift(Elevator lift){
		plugin.logDebug("Halting lift");
		for (Block b : lift.glassBlocks)
			b.setType(plugin.floorBlock);
		for (Entity p : lift.passengers){
			plugin.fallers.remove(p);
			if (p instanceof Player){
				Player pl = (Player) p;
				if (!plugin.flyers.contains(pl))
					pl.setAllowFlight(false);
				//((Player) p).setAllowFlight(plugin.serverFlight);
				if (plugin.useAntiCheat)
					AnticheatAPI.unexemptPlayer((Player) p, CheckType.FLY);
			}
			if (plugin.useSpout){
				if (p instanceof Player){
					SpoutManager.getPlayer((Player) p).setGravityMultiplier(1);
					SpoutManager.getPlayer((Player) p).setCanFly(false);
				}					
			}
		}
		lifts.remove(lift);
		plugin.getServer().getScheduler().cancelTask(lift.taskid);
		lift.clear();
	}
	
	public static void removePlayer(Player player){
		for (Elevator elevator : lifts){
			if (elevator.passengers.contains(player))
				elevator.passengers.remove(player);
			if (plugin.fallers.contains(player))
				plugin.fallers.remove(player);
			if (plugin.flyers.contains(player))
				plugin.flyers.remove(player);
		}
	}
	
	public static boolean isBaseBlock(Block block){
		if (plugin.blockSpeeds.containsKey(block.getType()))
			return true;
		return false;
	}
	
}
