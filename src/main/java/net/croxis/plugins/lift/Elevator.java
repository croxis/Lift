package net.croxis.plugins.lift;

import java.util.ArrayList;
import java.util.TreeMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.getspout.spoutapi.SpoutManager;

public class Elevator implements Runnable {
	private ArrayList<Block> floorBlocks = new ArrayList<Block>();
	private TreeMap <Integer, Floor> floormap = new TreeMap<Integer, Floor>();//Index is y value
	private TreeMap <Integer, Floor> floormap2 = new TreeMap<Integer, Floor>();//Index is floor value
	private ArrayList<Player> passengers = new ArrayList<Player>();
	public int destinationY = 0;//Destination y coordinate
	public ArrayList<Block> glassBlocks = new ArrayList<Block>();
	public int taskid = 0;
	private Lift plugin;

	public Elevator(Lift plugin, Block block) {
		this.plugin = plugin;
		//ID the iron base block. 
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
				if (plugin.debug)
					System.out.println("There is an obstruction");
				return;
			}
		}
		
		if (plugin.debug)
			System.out.println("Elevator area: " + floorBlocks.size());
		
		//Count all blocks up from base and make sure no obstructions to top floor
		//Identify floors
		
		for (Block b : floorBlocks){
			int x = b.getX();
			int z = b.getZ();
			int y1 = b.getY();
			
			while (true){
				y1 = y1 + 1;
				Block testBlock = b.getWorld().getBlockAt(x, y1, z);
				if (plugin.debug)
					System.out.println("Is valid block: " + isValidBlock(testBlock) + " at " + testBlock.getLocation());
				if (!isValidBlock(testBlock))
					break;
				if (plugin.debug)
					System.out.println("Yes I did make it this far");
				if (testBlock.getType() == Material.STONE_BUTTON){
					if (plugin.debug)
						System.out.println("Button found at: " + testBlock.getLocation());
					if (testBlock.getRelative(BlockFace.DOWN, 2).getType() == Material.GLASS 
							|| testBlock.getRelative(BlockFace.DOWN, 2).getType() == Material.IRON_BLOCK){
						Floor floor = new Floor();
						floor.setY(y1);
						if (testBlock.getRelative(BlockFace.DOWN).getType() == Material.WALL_SIGN)
							floor.setName(((Sign) testBlock.getRelative(BlockFace.DOWN)).getLine(1));
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
		//Elevator is contructed, pass off to check signs for floor desitnation, collect all people and move them
		
	}
	
	//Checks if block is a valid elevator block SANS iron
	public boolean isValidBlock(Block checkBlock){
		if (checkBlock.getType() == Material.AIR || checkBlock.getType() == Material.GLASS 
				|| checkBlock.getType() == Material.TORCH || checkBlock.getType() == Material.WALL_SIGN
				|| checkBlock.getType() == Material.STONE_BUTTON)
			return true;
		return false;
	}
	
	
	//Recursive function that constructs our list of blocks
	public void scanFloorBlocks(Block block){
		//NASTY DEBUG!
		System.out.println("Floor check");
		if (floorBlocks.size() >= 15)
			return; //5x5 max, prevents infinite loops
		else if (floorBlocks.contains(block))
			return; // We have that block already
		floorBlocks.add(block);
		
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
	
	public boolean isInShaft(Player player){
		for (Block block : floorBlocks){
			Location inside = block.getLocation();
			Location loc = player.getLocation();
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
	
	public boolean isInShaftAtFloor(Player player, Floor floor){
		if (isInShaft(player)){
			if (player.getLocation().getY() >= floor.getY() - 1 && player.getLocation().getY() <= floor.getY())
				return true;
		}
		return false;
	}

	public ArrayList<Block> getFloorBlocks() {
		return floorBlocks;
	}
	
	public void addPassenger(Player entity){
		passengers.add(entity);
	}
	
	public void setPassengers(ArrayList<Player> entities){
		passengers.clear();
		passengers.addAll(entities);
	}
	
	public ArrayList<Player> getPassengers(){
		return passengers;
	}

	public void run() {
		if(passengers.get(0).getLocation().getY() < destinationY && passengers.get(0).getLocation().getY() > destinationY-2){
			for (Player p : passengers){
				p.setVelocity(new Vector(0,0,0));
			}
			for (Block b : glassBlocks){
				b.setType(Material.GLASS);
			}
			for (Player p : passengers){
				SpoutManager.getPlayer(p).setGravityMultiplier(1);
			}
			plugin.getServer().getScheduler().cancelTask(taskid);
		}
	}

}


