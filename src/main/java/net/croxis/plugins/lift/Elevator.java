package net.croxis.plugins.lift;

import java.util.ArrayList;
import java.util.TreeMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
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
	public ArrayList<Block> floorBlocks = new ArrayList<Block>();
	private TreeMap <Integer, Floor> floormap = new TreeMap<Integer, Floor>();//Index is y value
	private TreeMap <Integer, Floor> floormap2 = new TreeMap<Integer, Floor>();//Index is floor value
	public ArrayList<LivingEntity> passengers = new ArrayList<LivingEntity>();
	public int destinationY = 0;//Destination y coordinate
	public ArrayList<Block> glassBlocks = new ArrayList<Block>();
	public int taskid = 0;
	public Floor destFloor = null;
	public Floor startFloor = null;
	private Lift plugin;
	public boolean goingUp = false;
	public ArrayList<Chunk> chunks = new ArrayList<Chunk>();

	public Elevator(Lift plugin, Block block) {
		long startTime = System.currentTimeMillis();
		this.plugin = plugin;
		//ID the iron base block. 
		if (plugin.debug)
			System.out.println("Starting elevator gen");
		int yd = 2;
		while(block.getY() - yd >= 0){
			if (block.getY() - yd == 0) //Gone too far with no base abort!
				return;
			Block checkBlock = block.getWorld().getBlockAt(block.getX(), block.getY()-yd, block.getZ());
			if (isValidBlock(checkBlock)){
				// Do nothing keep going
			} else if (checkBlock.getType() == Material.IRON_BLOCK) {
				scanFloorBlocks(checkBlock);
				break;
			} else {
				// Something is obstructing the elevator so stop
				return;
			}
			yd = yd + 1;
		}
		
		//Count all blocks up from base and make sure no obstructions to top floor
		//Identify floors
		
		for (Block b : floorBlocks){
			int x = b.getX();
			int z = b.getZ();
			int y1 = b.getY();
			
			while (true){
				y1 = y1 + 1;
				Block testBlock = b.getWorld().getBlockAt(x, y1, z);
				//if (plugin.debug)
				//	System.out.println("Is valid block: " + isValidBlock(testBlock) + " at " + testBlock.getLocation());
				if (!isValidBlock(testBlock))
					break;
				//if (plugin.debug)
				//	System.out.println("Yes I did make it this far");
				if (testBlock.getType() == Material.STONE_BUTTON){
					if (testBlock.getRelative(BlockFace.DOWN, 2).getType() == Material.GLASS 
							|| testBlock.getRelative(BlockFace.DOWN, 2).getType() == Material.IRON_BLOCK){
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
		}
		
		//Count all floors and order them -- Not needed due to treemap?
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
	public boolean isValidBlock(Block checkBlock){
		if (checkBlock.getType() == Material.AIR || checkBlock.getType() == Material.GLASS 
				|| checkBlock.getType() == Material.TORCH || checkBlock.getType() == Material.WALL_SIGN
				|| checkBlock.getType() == Material.STONE_BUTTON || checkBlock.getType() == Material.VINE)
			return true;
		return false;
	}
	
	
	//Recursive function that constructs our list of blocks
	public void scanFloorBlocks(Block block){
		if (floorBlocks.size() >= plugin.liftArea)
			return; //5x5 max, prevents infinite loops
		else if (floorBlocks.contains(block))
			return; // We have that block already
		floorBlocks.add(block);
		
		if (!chunks.contains(block.getChunk()))
			chunks.add(block.getChunk());
		
		Block checkBlock = block.getRelative(BlockFace.NORTH, 1);
		if (checkBlock.getType() == Material.IRON_BLOCK)
			scanFloorBlocks(checkBlock);
		checkBlock = block.getRelative(BlockFace.EAST, 1);
		if (checkBlock.getType() == Material.IRON_BLOCK)
			scanFloorBlocks(checkBlock);
		checkBlock = block.getRelative(BlockFace.SOUTH, 1);
		if (checkBlock.getType() == Material.IRON_BLOCK)
			scanFloorBlocks(checkBlock);
		checkBlock = block.getRelative(BlockFace.WEST, 1);
		if (checkBlock.getType() == Material.IRON_BLOCK)
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
					(loc.getY() <= floormap.get(floormap.lastKey()).getY()) && 
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
	
	public ArrayList<LivingEntity> getPassengers(){
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
			if (destFloor.getY() > startFloor.getY())
				p.setVelocity(new Vector(0.0D, plugin.liftSpeed, 0.0D));
			else
				p.setVelocity(new Vector(0.0D, -plugin.liftSpeed, 0.0D));
		}
		
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
		
		if (count >= passengers.size())
			endLift();
	}
	
	public boolean isInLift(Player player){
		return passengers.contains(player);
	}

}


