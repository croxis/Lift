/*
 * This file is part of Lift.
 *
 * Copyright (c) ${project.inceptionYear}-2012, croxis <https://github.com/croxis/>
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

import org.spout.api.entity.Entity;
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
	private HashSet<Entity> passengers = new HashSet<Entity>();
	public int destinationY = 0;//Destination y coordinate
	public HashSet<Block> glassBlocks = new HashSet<Block>();
	public int taskid = 0;
	public Floor destFloor = null;
	public Floor startFloor = null;
	public boolean goingUp = false;
	public HashSet<Chunk> chunks = new HashSet<Chunk>();
	private HashMap<Entity, Point> holders = new HashMap<Entity, Point>();
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
			Point loc = entity.getScene().getPosition();
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
			if (entity.getScene().getPosition().getY() >= floor.getY() - 1 && entity.getScene().getPosition().getY() <= floor.getY())
				return true;
		}
		return false;
	}
	
	public void addPassenger(Entity entity){
		passengers.add(entity);
	}
	
	public void addHolder(Entity entity, Point location){
		holders.put(entity, location);
	}
	
	public void setPassengers(ArrayList<Entity> entities){
		passengers.clear();
		passengers.addAll(entities);
	}
	
	public int getTotalFloors(){
		return floormap2.size();
	}
	
	public boolean isInLift(Entity player){
		return passengers.contains(player);
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
	
	public Point getHolderPos(Entity entity){
		return holders.get(entity);
	}
	
	public int getSize(){
		return passengers.size() + holders.size();
	}
}