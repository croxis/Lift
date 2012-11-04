package net.croxis.plugins.lift;

import java.util.HashSet;
import java.util.Iterator;

import net.h31ix.anticheat.api.AnticheatAPI;
import net.h31ix.anticheat.manage.CheckType;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.getspout.spoutapi.SpoutManager;

public class ElevatorManager implements Runnable {
	private static Lift plugin;
	public static HashSet<Elevator> elevators = new HashSet<Elevator>();
	public static HashSet<Entity> fallers = new HashSet<Entity>();
	public static HashSet<Player> flyers = new HashSet<Player>();
	private int taskid;

	public ElevatorManager(Lift plugin) {
		ElevatorManager.plugin = plugin;
		taskid = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 2, 2);
	}
	
	public static Elevator createLift(Block block){
		long startTime = System.currentTimeMillis();
		plugin.logDebug("Starting elevator gen");
		Elevator elevator = new Elevator();
		int yscan = block.getY() - 1;
		while(yscan >= plugin.lowScan){
			if (yscan == plugin.lowScan){ //Gone too far with no base abort!
				plugin.logDebug("yscan was 0");
				return null;
			}
			Block checkBlock = block.getWorld().getBlockAt(block.getX(), yscan, block.getZ());
			if (isValidShaftBlock(checkBlock)){
				// Do nothing keep going
			} else if (ElevatorManager.isBaseBlock(checkBlock)) {
				elevator.baseBlockType = checkBlock.getType();
				elevator.speed = plugin.blockSpeeds.get(elevator.baseBlockType);
				scanBaseBlocks(checkBlock, elevator);
				for (Block b : elevator.baseBlocks){
					// This is for speed optimization for entering lift in use
					if (!elevator.chunks.contains(b.getChunk()))
						elevator.chunks.add(b.getChunk());
				}
				break;
			} else {
				// Something is obstructing the elevator so stop
				if (Lift.debug){
					System.out.println("==Unknown Error==");
					System.out.println("Yscan: " + Integer.toString(yscan));
					System.out.println("Block: " + checkBlock.getType().toString());
					System.out.println("Is Valid Block: " + Boolean.toString(isValidShaftBlock(checkBlock)));
					System.out.println("Is Base Block: " + Boolean.toString(ElevatorManager.isBaseBlock(checkBlock)));
				}
				return null;
			}
			yscan--;
		}
		plugin.logDebug("Base size: " + Integer.toString(elevator.baseBlocks.size()));
		
		constructFloors(elevator);
		
		//Elevator is constructed, pass off to check signs for floor destination, collect all people and move them
		plugin.logDebug("Elevator gen took: " + (System.currentTimeMillis() - startTime) + " ms.");
		return elevator;
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
	//I'd rather it just return a hashset instead of passing elevator
	//But I can't figure out a clean way to do it
	public static void scanBaseBlocks(Block block, Elevator elevator){
		if (elevator.baseBlocks.size() >= plugin.liftArea)
			return; //5x5 max, prevents infinite loops
		else if (elevator.baseBlocks.contains(block))
			return; // We have that block already
		elevator.baseBlocks.add(block);
		if (block.getRelative(BlockFace.NORTH, 1).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(BlockFace.NORTH), elevator);
		if (block.getRelative(BlockFace.EAST, 1).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(BlockFace.EAST), elevator);
		if (block.getRelative(BlockFace.SOUTH, 1).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(BlockFace.SOUTH), elevator);
		if (block.getRelative(BlockFace.WEST, 1).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(BlockFace.WEST), elevator);
		return;
	}
	
	public static String constructFloors(Elevator elevator){
		String message = "";
		int maxY = 0;

		for (Block b : elevator.baseBlocks){
			int x = b.getX();
			int z = b.getZ();
			int y1 = b.getY();
			
			World currentWorld = b.getWorld();
			
			while (true){
				y1 = y1 + 1;
				if (y1 == plugin.highScan) {
					break;
				}
				Block testBlock = b.getWorld().getBlockAt(x, y1, z);
				if (!isValidShaftBlock(testBlock)){
					message += " | " + x + " " + y1 + " " + z + " of type "  + testBlock.getType().toString();
					maxY = y1;
					break;
				}
				if (testBlock.getType() == Material.STONE_BUTTON){
					if (plugin.checkGlass)
						if (!scanFloorAtY(currentWorld, testBlock.getY() - 2, elevator)){
							break;
						}
					Floor floor = new Floor();
					floor.setY(y1);
					if (testBlock.getRelative(BlockFace.DOWN).getType() == Material.WALL_SIGN)
						floor.setName(((Sign) testBlock.getRelative(BlockFace.DOWN).getState()).getLine(1));
					if (testBlock.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN)
						elevator.floormap.put(y1, floor);
					plugin.logDebug("Floor added: " + b.getLocation());
				}
			}
		}
		int floorNumber = 1;
		for (Floor floor : elevator.floormap.values()){
			floor.setFloor(floorNumber);
			elevator.floormap2.put(floorNumber, floor);
			floorNumber = floorNumber + 1;
		}
		return message;
	}
	
	public static boolean scanFloorAtY(World world, int y, Elevator elevator){
		for (Block block : elevator.baseBlocks){
			if (Lift.debug){
				System.out.println("Scan glass block type: " + world.getBlockAt(block.getX(), y, block.getZ()).getType().toString());
				System.out.println("Is not glass?: " + Boolean.toString(world.getBlockAt(block.getX(), y, block.getZ()).getType() != plugin.floorBlock));
				System.out.println("Is not base?: " + Boolean.toString(!plugin.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())));
			}
			if (world.getBlockAt(block.getX(), y, block.getZ()).getType() != plugin.floorBlock && !plugin.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())){
				if (Lift.debug)
					System.out.println("Invalid block type");
				return false;	
			}
		}
		return true;
	}
	
	public static void endLift(Elevator elevator){
		plugin.logDebug("Halting lift");
		for (Block b : elevator.glassBlocks)
			b.setType(plugin.floorBlock);
		for (LivingEntity p : elevator.passengers){
			fallers.remove(p);
			if (p instanceof Player){
				Player pl = (Player) p;
				if (!flyers.contains(pl))
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
		elevator.clear();
		//elevators.remove(elevator);
		//plugin.getServer().getScheduler().cancelTask(elevator.taskid);
	}
	
	public static void removePlayer(Player player, Iterator<LivingEntity> passengers){
		for (Elevator elevator : elevators){
			if (elevator.passengers.contains(player)){
				//elevator.passengers.remove(player);
				passengers.remove();
				if (fallers.contains(player))
					fallers.remove(player);
				if (flyers.contains(player))
					flyers.remove(player);
				else
					player.setAllowFlight(false);
			}
		}
	}
	
	public static void removePlayer(Player player){
		for (Elevator elevator : elevators){
			if (elevator.passengers.contains(player)){
				elevator.passengers.remove(player);
				if (fallers.contains(player))
					fallers.remove(player);
				if (flyers.contains(player))
					flyers.remove(player);
				else
					player.setAllowFlight(false);
			}
		}
	}
	
	public static boolean isBaseBlock(Block block){
		if (plugin.blockSpeeds.containsKey(block.getType()))
			return true;
		return false;
	}
	
	public static boolean isPassenger(Entity entity){
		Iterator<Elevator> iterator = elevators.iterator();
		while (iterator.hasNext()){
			Elevator elevator = iterator.next();
			if (elevator.passengers.contains(entity))
				return true;
		}
		return false;
	}
	
	public void run() {
		//Using while loop iterator so we can remove lifts in a sane way
		Iterator<Elevator> eleviterator = elevators.iterator();
		//for (Elevator e : elevators){
		while (eleviterator.hasNext()){
			Elevator e = eleviterator.next();
			if(e.passengers.isEmpty()){
				ElevatorManager.endLift(e);
				eleviterator.remove();
				continue;
			}
			
			//Re apply impulse as it does seem to run out
			for (Entity p : e.getPassengers()){
				if(e.destFloor.getFloor() > e.startFloor.getFloor())
					p.setVelocity(new Vector(0.0D, e.speed, 0.0D));
				else
					p.setVelocity(new Vector(0.0D, -e.speed, 0.0D));
				p.setFallDistance(0.0F);
			}
			
			Iterator<LivingEntity> passengers = e.passengers.iterator();
			while (passengers.hasNext()){
				LivingEntity passenger = passengers.next();
				
				//Check if passengers have left the shaft
				if (!e.isInShaft(passenger) && e instanceof Player)
					removePlayer((Player) passenger, passengers);
				
				if((e.goingUp && passenger.getLocation().getY() > e.destFloor.getY()-1)
						|| (!e.goingUp && passenger.getLocation().getY() < e.destFloor.getY()-0.1)){
					passenger.setVelocity(new Vector(0,0,0));
					Location pLoc = passenger.getLocation();
					pLoc.setY(e.destFloor.getY()-0.7);
					passenger.teleport(pLoc);
					e.holders.put(passenger, passenger.getLocation());
					if (e instanceof Player)
						removePlayer((Player) passenger, passengers);
					else
						passengers.remove();
				}
			}
			
			for (LivingEntity holder : e.holders.keySet()){
				holder.teleport(e.holders.get(holder));
				holder.setFallDistance(0.0F);
			}
		}
	}
}
