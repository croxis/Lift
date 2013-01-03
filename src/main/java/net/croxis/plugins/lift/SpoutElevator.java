package net.croxis.plugins.lift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.spout.api.component.impl.TransformComponent;
import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.geo.cuboid.Chunk;
import org.spout.api.geo.discrete.Point;
import org.spout.api.material.Material;

public class SpoutElevator {
	public HashSet<Block> baseBlocks = new HashSet<Block>();
	public TreeMap <Integer, Floor> floormap = new TreeMap<Integer, Floor>();//Index is y value
	public TreeMap <Integer, Floor> floormap2 = new TreeMap<Integer, Floor>();//Index is floor value
	private TreeMap <World, TreeMap<Integer, Floor>> worldFloorMap= new TreeMap <World, TreeMap<Integer, Floor>>();
	public HashSet<Entity> passengers = new HashSet<Entity>();
	public int destinationY = 0;//Destination y coordinate
	public HashSet<Block> glassBlocks = new HashSet<Block>();
	public int taskid = 0;
	public Floor destFloor = null;
	public Floor startFloor = null;
	public boolean goingUp = false;
	public HashSet<Chunk> chunks = new HashSet<Chunk>();
	public HashMap<Entity, Point> holders = new HashMap<Entity, Point>();
	public Material baseBlockType = Material.get("IRON_BLOCK");
	public double speed = 0.5;
	
	public void clear(){
		baseBlocks.clear();
		floormap.clear();
		floormap2.clear();
		worldFloorMap.clear();
		passengers.clear();
		glassBlocks.clear();
		holders.clear();
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
			Point inside = block.getPosition();
			Point loc = entity.get(TransformComponent.class).getPosition();
			if (loc.getBlockX() == block.getX() && 
					(loc.getY() >= inside.getY() - 1.0D) && 
					(loc.getY() <= floormap2.get(floormap2.lastKey()).getY()) && 
					loc.getBlockZ() == block.getZ())
				return true;
		}
		return false;
	}
	
	public boolean isInShaftAtFloor(Entity entity, Floor floor){
		if (isInShaft(entity)){
			if (entity.get(TransformComponent.class).getPosition().getY() >= floor.getY() - 1 && entity.get(TransformComponent.class).getPosition().getY() <= floor.getY())
				return true;
		}
		return false;
	}
	
	public void addPassenger(Entity entity){
		passengers.add(entity);
	}
	
	public void setPassengers(ArrayList<Entity> entities){
		passengers.clear();
		passengers.addAll(entities);
	}
	
	public HashSet<Entity> getPassengers(){
		return passengers;
	}
	
	public int getTotalFloors(){
		return floormap2.size();
	}
	
	public boolean isInLift(Player player){
		return passengers.contains(player);
	}
}