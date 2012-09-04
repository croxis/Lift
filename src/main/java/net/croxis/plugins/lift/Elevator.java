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
	
	public void clear(){
		floorBlocks.clear();
		floormap.clear();
		floormap2.clear();
		worldFloorMap.clear();
		passengers.clear();
		glassBlocks.clear();
	}
	
	
	
	public boolean scanGlassAtY(World world, int y){
		for (Block block : floorBlocks){
			if (plugin.debug){
				System.out.println("Scan glass block type: " + world.getBlockAt(block.getX(), y, block.getZ()).getType().toString());
				System.out.println("Is not glass?: " + Boolean.toString(world.getBlockAt(block.getX(), y, block.getZ()).getType() != plugin.floorBlock));
				System.out.println("Is not base?: " + Boolean.toString(!plugin.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())));
			}
			if (world.getBlockAt(block.getX(), y, block.getZ()).getType() != plugin.floorBlock && !plugin.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())){
				if (plugin.debug)
					System.out.println("Invalid block type");
				return false;	
			}
		}
		return true;
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

	public void run() {
		if(passengers.isEmpty()){
			ElevatorManager.endLift(this);
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
			ElevatorManager.endLift(this);
	}
	
	public boolean isInLift(Player player){
		return passengers.contains(player);
	}
}


