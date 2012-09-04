package net.croxis.plugins.lift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Elevator {
	public HashSet<Block> baseBlocks = new HashSet<Block>();
	public TreeMap <Integer, Floor> floormap = new TreeMap<Integer, Floor>();//Index is y value
	public TreeMap <Integer, Floor> floormap2 = new TreeMap<Integer, Floor>();//Index is floor value
	private TreeMap <World, TreeMap<Integer, Floor>> worldFloorMap= new TreeMap <World, TreeMap<Integer, Floor>>();
	public HashSet<LivingEntity> passengers = new HashSet<LivingEntity>();
	public int destinationY = 0;//Destination y coordinate
	public HashSet<Block> glassBlocks = new HashSet<Block>();
	public int taskid = 0;
	public Floor destFloor = null;
	public Floor startFloor = null;
	public boolean goingUp = false;
	public HashSet<Chunk> chunks = new HashSet<Chunk>();
	public HashMap<LivingEntity, Location> holders = new HashMap<LivingEntity, Location>();
	public Material baseBlockType = Material.IRON_BLOCK;
	public double speed = 0.5;
	
	public void clear(){
		baseBlocks.clear();
		floormap.clear();
		floormap2.clear();
		worldFloorMap.clear();
		passengers.clear();
		glassBlocks.clear();
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
		for (Block block : baseBlocks){
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
	
	public boolean isInLift(Player player){
		return passengers.contains(player);
	}
}


