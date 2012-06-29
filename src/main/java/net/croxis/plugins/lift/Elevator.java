package net.croxis.plugins.lift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import net.h31ix.anticheat.api.AnticheatAPI;
import net.h31ix.anticheat.manage.CheckType;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
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
//import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.SpoutManager;

public class Elevator implements Runnable {
	public HashSet<Block> floorBlocks = new HashSet<Block>();
	private TreeMap <Integer, Floor> floormap = new TreeMap<Integer, Floor>();//Index is y value
	private TreeMap <Integer, Floor> floormap2 = new TreeMap<Integer, Floor>();//Index is floor value
	private TreeMap <World, TreeMap<Integer, Floor>> worldFloorMap= new TreeMap <World, TreeMap<Integer, Floor>>();
	public HashSet<LivingEntity> passengers = new HashSet<LivingEntity>();
	public int destinationY = 0;//Destination y coordinate
	public HashSet<Block> glassBlocks = new HashSet<Block>();
	public int taskid = 0;
	public Floor destFloor = null;
	public Floor startFloor = null;
	private Lift plugin;
	public boolean goingUp = false;
	public HashSet<Chunk> chunks = new HashSet<Chunk>();
	public HashMap<LivingEntity, Location> holders = new HashMap<LivingEntity, Location>();
	public Material baseBlockType = Material.IRON_BLOCK;
	public double speed = 0.5;

	public Elevator(Lift plugin, Block block) {
		long startTime = System.currentTimeMillis();
		this.plugin = plugin;
		//ID the iron base block. 
		if (plugin.debug)
			System.out.println("Starting elevator gen");
		//int yd = 2;
		int yscan = block.getY() - 1;
		/*if (plugin.useV10){
			World currentWorld = block.getWorld();
			while(yscan >= plugin.v10verlap_API.getMinY(currentWorld)){
				if (yscan == plugin.v10verlap_API.getMinY(currentWorld)){
					currentWorld = plugin.v10verlap_API.getLowerWorld(block.getWorld());
					if (currentWorld == null) //Gone too far with no base abort!
						return;
					yscan = plugin.v10verlap_API.getMaxY(currentWorld);
					Block checkBlock = currentWorld.getBlockAt(block.getX(), yscan, block.getZ());
					if (isValidBlock(checkBlock)){
						// Do nothing keep going
					} else if (isBaseBlock(checkBlock)) {
						scanFloorBlocks(checkBlock);
						break;
					} else {
						// Something is obstructing the elevator so stop
						return;
					}
					yscan--;
				}
			}
		} else {*/
			while(yscan >= 0){
				if (yscan == 0){ //Gone too far with no base abort!
					System.out.println("yscan was 0");
					return;
				}
				Block checkBlock = block.getWorld().getBlockAt(block.getX(), yscan, block.getZ());
				if (isValidBlock(checkBlock)){
					// Do nothing keep going
				} else if (isBaseBlock(checkBlock)) {
					scanFloorBlocks(checkBlock);
					break;
				} else {
					// Something is obstructing the elevator so stop
					if (plugin.debug){
						System.out.println("==Unknown Error==");
						System.out.println("Yscan: " + Integer.toString(yscan));
						System.out.println("Block: " + checkBlock.getType().toString());
						System.out.println("Is Valid Block: " + Boolean.toString(isValidBlock(checkBlock)));
						System.out.println("Is Base Block: " + Boolean.toString(isBaseBlock(checkBlock)));
					}
					return;
				}
				yscan--;
			}
		//}
		
		//Count all blocks up from base and make sure no obstructions to top floor
		//Identify floors
		
		if (plugin.debug)
			System.out.println("Base size: " + Integer.toString(floorBlocks.size()));
		
		for (Block b : floorBlocks){
			int x = b.getX();
			int z = b.getZ();
			int y1 = b.getY();
			
			yscan = b.getY();
			World currentWorld = b.getWorld();
			/*if (plugin.useV10){
				while (true){
					yscan++;
					if (yscan > plugin.v10verlap_API.getMaxY(currentWorld)){
						if (plugin.v10verlap_API.getUpperWorld(currentWorld) == null)
							break;
						currentWorld = plugin.v10verlap_API.getUpperWorld(currentWorld);
						Block testBlock = currentWorld.getBlockAt(x, yscan, z);
						if (!isValidBlock(testBlock))
							break;
						if (testBlock.getType() == Material.STONE_BUTTON){
							if (plugin.checkGlass)
								if (!scanGlassAtY(currentWorld, testBlock.getY() - 2))
									break;
							Floor floor = new Floor();
							floor.setY(yscan);
							floor.setWorld(currentWorld);
							if (testBlock.getRelative(BlockFace.DOWN).getType() == Material.WALL_SIGN)
								floor.setName(((Sign) testBlock.getRelative(BlockFace.DOWN).getState()).getLine(1));
							if (testBlock.getRelative(BlockFace.UP).getType() == Material.WALL_SIGN){
								if (worldFloorMap.containsKey(currentWorld)){
									worldFloorMap.get(currentWorld).put(yscan, floor);
								} else {
									TreeMap<Integer, Floor> map = new TreeMap<Integer, Floor>();
									map.put(y1, floor);
									worldFloorMap.put(currentWorld, map);
								}
							}
							if (plugin.debug)
								System.out.println("Floor added: " + b.getLocation());
						}
					}
				}
			} else {*/		
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
		//}
		
		//Count all floors and order them
		
		/*if(plugin.useV10){
			int floorNumber = 1;
			// First order the worlds from bottom to top in array/list
			// Then cycle through worlds in order to build floor order
			ArrayList<World> worlds = new ArrayList<World>();
			World currentWorld = block.getWorld();
			worlds.add(currentWorld);
			while(true){
				currentWorld = plugin.v10verlap_API.getLowerWorld(currentWorld);
				if (currentWorld == null)
					break;
				if (worlds.contains(currentWorld))
					break;
				worlds.add(0, currentWorld);
			}
			currentWorld = block.getWorld();
			while(true){
				currentWorld = plugin.v10verlap_API.getUpperWorld(currentWorld);
				if (currentWorld == null)
					break;
				if (worlds.contains(currentWorld))
					break;
				worlds.add(currentWorld);
			}
			for (World world : worlds){
				if (worldFloorMap.containsKey(world)){
					for (Floor floor : worldFloorMap.get(world).values()){
						floor.setFloor(floorNumber);
						floormap2.put(floorNumber, floor);
						floorNumber++;
					}
				}
			}
			
		} else {*/
			int floorNumber = 1;
			for (Floor floor : floormap.values()){
				floor.setFloor(floorNumber);
				floormap2.put(floorNumber, floor);
				floorNumber = floorNumber + 1;
			}
		//}
		
		
		//Elevator is constructed, pass off to check signs for floor destination, collect all people and move them
		if (plugin.debug)
			System.out.println("Elevator gen took: " + (System.currentTimeMillis() - startTime) + " ms.");
	}
	
	//Checks if block is a valid elevator block SANS iron
	public boolean isValidBlock(Block checkBlock){
		if (checkBlock.getType() == Material.AIR || checkBlock.getType() == Material.GLASS 
				|| checkBlock.getType() == Material.TORCH || checkBlock.getType() == Material.WALL_SIGN
				|| checkBlock.getType() == Material.STONE_BUTTON || checkBlock.getType() == Material.VINE || checkBlock.getType() == Material.LADDER)
			return true;
		return false;
	}
	
	public boolean scanGlassAtY(World world, int y){
		for (Block block : floorBlocks){
			if (plugin.debug){
				System.out.println("Scan glass block type: " + world.getBlockAt(block.getX(), y, block.getZ()).getType().toString());
				System.out.println("Is not glass?: " + Boolean.toString(world.getBlockAt(block.getX(), y, block.getZ()).getType() != Material.GLASS));
				System.out.println("Is not base?: " + Boolean.toString(!plugin.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())));
			}
			if (world.getBlockAt(block.getX(), y, block.getZ()).getType() != Material.GLASS && !plugin.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())){
				if (plugin.debug)
					System.out.println("Invalid block type");
				return false;	
			}
		}
		return true;
	}
	
	
	//Recursive function that constructs our list of blocks
	public void scanFloorBlocks(Block block){
		this.baseBlockType = block.getType();
		this.speed = plugin.blockSpeeds.get(baseBlockType);
		if (floorBlocks.size() >= plugin.liftArea)
			return; //5x5 max, prevents infinite loops
		else if (floorBlocks.contains(block))
			return; // We have that block already
		floorBlocks.add(block);
		
		if (!chunks.contains(block.getChunk()))
			chunks.add(block.getChunk());
		
		Block checkBlock = block.getRelative(BlockFace.NORTH, 1);
		if (checkBlock.getType() == baseBlockType)
			scanFloorBlocks(checkBlock);
		checkBlock = block.getRelative(BlockFace.EAST, 1);
		if (checkBlock.getType() == baseBlockType)
			scanFloorBlocks(checkBlock);
		checkBlock = block.getRelative(BlockFace.SOUTH, 1);
		if (checkBlock.getType() == baseBlockType)
			scanFloorBlocks(checkBlock);
		checkBlock = block.getRelative(BlockFace.WEST, 1);
		if (checkBlock.getType() == baseBlockType)
			scanFloorBlocks(checkBlock);
	}
	
	public TreeMap <Integer, Floor> getFloormap(){
		return floormap;
	}
	
	public TreeMap <Integer, Floor> getFloormap2(){
		return floormap2;
	}
	
	public Floor getFloorFromY(int y){
		return floormap.get(y);
	}
	
	public Floor getFloorFromN(int n){
		return floormap2.get(n);
	}
	
	public boolean isInShaft(Entity entity){
		for (Block block : floorBlocks){
			Location inside = block.getLocation();
			Location loc = entity.getLocation();
			if ((loc.getX() < inside.getX() + 1.0D) && 
					(loc.getX() > inside.getX() - 1.0D) && 
					(loc.getY() >= inside.getY() - 1.0D) && 
					(loc.getY() <= floormap2.get(floormap2.lastKey()).getY()) && 
					(loc.getZ() < inside.getZ() + 1.0D) && 
					(loc.getZ() > inside.getZ() - 1.0D))
				return true;
		}
		return false;
	}
	
	public boolean isInShaftAtFloor(Entity entity, Floor floor){
		if (isInShaft(entity)){
			if (entity.getLocation().getY() >= floor.getY() - 1 && entity.getLocation().getY() <= floor.getY())
				return true;
		}
		return false;
	}
	
	public void addPassenger(LivingEntity entity){
		passengers.add(entity);
	}
	
	public void setPassengers(ArrayList<LivingEntity> entities){
		passengers.clear();
		passengers.addAll(entities);
	}
	
	public HashSet<LivingEntity> getPassengers(){
		return passengers;
	}
	
	public int getTotalFloors(){
		return floormap2.size();
	}
	
	public void endLift(){
		if (plugin.debug)
			System.out.println("Halting lift");
		for (Block b : glassBlocks)
			b.setType(Material.GLASS);
		for (Entity p : this.passengers){
			plugin.fallers.remove(p);
			if (p instanceof Player){
				Player pl = (Player) p;
				if (pl.getGameMode() == GameMode.SURVIVAL)
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
		plugin.lifts.remove(this);
		plugin.getServer().getScheduler().cancelTask(taskid);
	}

	public void run() {
		if(passengers.isEmpty()){
			endLift();
			return;
		}
		
		//Re apply impulse as it does seem to run out
		for (Entity p : getPassengers()){
			//if (destFloor.getY() > startFloor.getY())
			if(destFloor.getFloor() > startFloor.getFloor())
				p.setVelocity(new Vector(0.0D, speed, 0.0D));
			else
				p.setVelocity(new Vector(0.0D, -speed, 0.0D));
			p.setFallDistance(0.0F);
		}
		
		/*if(plugin.useV10){
			
			int count = 0;
			for (Entity passenger : passengers){
				Location pLoc = passenger.getLocation();
				if(passenger.getWorld() == destFloor.getWorld())
					if((goingUp && pLoc.getY() > destFloor.getY()-1)
							|| (!goingUp && pLoc.getY() < destFloor.getY()-0.1)){
						count++;
						passenger.setVelocity(new Vector(0,0,0));
						pLoc.setY(destFloor.getY()-0.7);
						passenger.teleport(pLoc);
					}
			}
			
			for (LivingEntity holder : holders.keySet()){
				holder.teleport(holders.get(holder));
				holder.setFallDistance(0.0F);
			}
			
			if (count >= passengers.size())
				endLift();
			
		} else {
			*/
			int count = 0;
			for (Entity passenger : passengers){
				Location pLoc = passenger.getLocation();
				if((goingUp && pLoc.getY() > destFloor.getY()-1)
						|| (!goingUp && pLoc.getY() < destFloor.getY()-0.1)){
					count++;
					passenger.setVelocity(new Vector(0,0,0));
					pLoc.setY(destFloor.getY()-0.7);
					passenger.teleport(pLoc);
				}
			}
			
			for (LivingEntity holder : holders.keySet()){
				holder.teleport(holders.get(holder));
				holder.setFallDistance(0.0F);
			}
			
			if (count >= passengers.size())
				endLift();
		}
	//}
	
	public boolean isInLift(Player player){
		return passengers.contains(player);
	}
	
	public boolean isBaseBlock(Block block){
		if (plugin.blockSpeeds.containsKey(block.getType()))
			return true;
		return false;
	}

}


