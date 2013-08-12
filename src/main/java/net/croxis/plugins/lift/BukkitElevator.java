/*
 * This file is part of Lift.
 *
 * Copyright (c) ${project.inceptionYear}-2013, croxis <https://github.com/croxis/>
 *
 * Lift is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Lift is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Lift. If not, see <http://www.gnu.org/licenses/>.
 */
package net.croxis.plugins.lift;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class BukkitElevator extends Elevator{
	public HashSet<Block> baseBlocks = new HashSet<Block>();
	public TreeMap <World, TreeMap<Integer, Floor>> worldFloorMap= new TreeMap <World, TreeMap<Integer, Floor>>();
	private HashSet<Entity> passengers = new HashSet<Entity>();
	private HashMap<Entity, Location> holders = new HashMap<Entity, Location>();
	//public HashSet<Block> glassBlocks = new HashSet<Block>();
	private HashMap<Location, FloorBlock> floorBlocks = new HashMap<Location, FloorBlock>();
	//Integer is the meta "damage" value
	private HashMap<Location, Byte> carpetBlocks = new HashMap<Location, Byte>();
	private HashMap<Location, Byte> railBlocks = new HashMap<Location, Byte>();
	public HashSet<Chunk> chunks = new HashSet<Chunk>();
	public Material baseBlockType = Material.IRON_BLOCK;
	public TreeMap <Integer, Floor> floormap = new TreeMap<Integer, Floor>();//Index is y value
	public TreeMap <Integer, Floor> floormap2 = new TreeMap<Integer, Floor>();//Index is floor value
	public Floor destFloor = null;
	public Floor startFloor = null;
	
	class FloorBlock{
		public Material material;
		public Byte data;
		public FloorBlock(final Material m, final Byte d){
			material = m;
			data = d;
		}
	}
	
	public void clear(){
		baseBlocks.clear();
		floormap.clear();
		floormap2.clear();
		worldFloorMap.clear();
		passengers.clear();
		floorBlocks.clear();
		carpetBlocks.clear();
		railBlocks.clear();
		holders.clear();
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
			if (loc.getBlockX() == block.getX() && 
					(loc.getY() >= inside.getY() - 1.0D) && 
					(loc.getY() <= floormap2.get(floormap2.lastKey()).getY() + 3.0D) && 
					loc.getBlockZ() == block.getZ())
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
	
	public void addPassenger(Entity entity){
		passengers.add(entity);
	}
	
	public void addHolder(Entity entity, Location location){
		holders.put(entity, location);
	}
	
	public void setPassengers(ArrayList<LivingEntity> entities){
		passengers.clear();
		passengers.addAll(entities);
	}
	
	public int getTotalFloors(){
		return floormap2.size();
	}
	
	public boolean isInLift(Entity entity){
		return (passengers.contains(entity) || holders.containsKey(entity));
	}
	
	public void removePassenger(Entity passenger){
		// NOt thread safe in an interation!
		if (passengers.contains(passenger))
			passengers.remove(passenger);
		else if (holders.containsKey(passenger))
			holders.remove(passenger);
	}
	
	public Iterator<Entity> getPassengers(){
		return passengers.iterator();
	}
	
	public Iterator<Entity> getHolders(){
		return holders.keySet().iterator();
	}
	
	public Location getHolderPos(Entity entity){
		return holders.get(entity);
	}
	
	public int getSize(){
		return passengers.size() + holders.size();
	}
	
	public HashMap<Location, FloorBlock> getFloorBlocks(){
		return floorBlocks;
	}
	
	public void addFloorBlock(Block block){
		floorBlocks.put(block.getLocation(), new FloorBlock(block.getType(), block.getData()));
	}
	
	public HashMap<Location, Byte> getCarpetBlocks(){
		return carpetBlocks;
	}
	
	public void addCarpetBlock(Block block){
		carpetBlocks.put(block.getLocation(), block.getData());
	}
}


