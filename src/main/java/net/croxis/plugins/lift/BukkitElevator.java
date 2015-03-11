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
import org.bukkit.entity.Minecart;
import org.bukkit.util.Vector;

public class BukkitElevator extends Elevator{
	public HashSet<Block> baseBlocks = new HashSet<Block>();
	public TreeMap <World, TreeMap<Integer, Floor>> worldFloorMap= new TreeMap <World, TreeMap<Integer, Floor>>();
	private HashSet<Entity> passengers = new HashSet<Entity>();
	private HashMap<Entity, Location> holders = new HashMap<Entity, Location>();
	//public HashSet<Block> glassBlocks = new HashSet<Block>();
	private HashMap<Location, FloorBlock> floorBlocks = new HashMap<Location, FloorBlock>();
	//Integer is the meta "damage" value
	private HashSet<Location> redstoneBlocks = new HashSet<Location>();
	private HashMap<Location, Byte> carpetBlocks = new HashMap<Location, Byte>();
	private HashMap<Location, FloorBlock> railBlocks = new HashMap<Location, FloorBlock>();
	private HashMap<Entity, Vector> minecartSpeeds = new HashMap<Entity, Vector>();
	public HashSet<Chunk> chunks = new HashSet<Chunk>();
	public Material baseBlockType = Material.IRON_BLOCK;
	
	public BukkitFloor destFloor = null;
	public BukkitFloor startFloor = null;
	
	class FloorBlock{
		public Material material;
		public Byte data;
		public FloorBlock(final Material m, final Byte d){
			material = m;
			data = d;
		}
	}
	
	public void clear(){
		super.clear();
		baseBlocks.clear();
		worldFloorMap.clear();
		passengers.clear();
		floorBlocks.clear();
		carpetBlocks.clear();
		railBlocks.clear();
		redstoneBlocks.clear();
		minecartSpeeds.clear();
		holders.clear();
	}
	
	public BukkitFloor getFloorFromY(int y){
		return (BukkitFloor) super.getFloorFromY(y);
	}
	
	public BukkitFloor getFloorFromN(int n){
		return (BukkitFloor) super.getFloorFromN(n);
	}
	
	public boolean isInShaft(Entity entity){
		for (Block block : baseBlocks){
			if (entity.getLocation().getY() >= block.getLocation().getY() - 1.0D &&
					entity.getLocation().getY() <= floormap2.get(floormap2.lastKey()).getY() + 3.0D &&
					entity.getLocation().getBlockX() == block.getX() &&
					entity.getLocation().getBlockZ() == block.getZ())
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
	
	public HashMap<Location, FloorBlock> getRailBlocks(){
		return railBlocks;
	}
	
	public void addRailBlock(Block block){
		railBlocks.put(block.getLocation(), new FloorBlock(block.getType(), block.getData()));
	}
	
	public HashSet<Location> getRedstoneBlocks(){
		return redstoneBlocks;
	}
	
	public void addRedstoneBlock(Block block){
		redstoneBlocks.add(block.getLocation());
	}
	
	public HashMap<Entity, Vector> getMinecartSpeeds(){
		return minecartSpeeds;
	}
	
	public void addMinecartSpeed(Minecart minecart){
		minecartSpeeds.put(minecart, minecart.getVelocity());
	}
}


