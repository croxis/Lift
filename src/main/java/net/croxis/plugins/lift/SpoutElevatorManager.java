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

import java.util.HashSet;
import java.util.Iterator;

import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.geo.World;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFace;
import org.spout.api.scheduler.Task;
import org.spout.api.scheduler.TaskPriority;
import org.spout.vanilla.plugin.component.living.neutral.Human;
import org.spout.vanilla.plugin.material.VanillaMaterials;
import org.spout.vanilla.plugin.component.substance.material.Sign;

public class SpoutElevatorManager extends ElevatorManager{
	private static SpoutLift plugin;
	public static HashSet<SpoutElevator> elevators = new HashSet<SpoutElevator>();
	public static HashSet<Entity> fallers = new HashSet<Entity>();
	public static HashSet<Entity> flyers = new HashSet<Entity>();
	public static Task spouttaskid;

	public SpoutElevatorManager(SpoutLift plugin) {
		SpoutElevatorManager.plugin = plugin;
		//TODO:Move to using getParallelTaskManager
		spouttaskid = plugin.getEngine().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 500, TaskPriority.NORMAL);
	}
	
	public static SpoutElevator createLift(Block block){
		long startTime = System.currentTimeMillis();
		plugin.logDebug("Starting elevator gen");
		SpoutElevator elevator = new SpoutElevator();
		int yscan = block.getY() - 1;
		while(yscan >= plugin.lowScan){
			if (yscan == plugin.lowScan){ //Gone too far with no base abort!
				plugin.logDebug("yscan was too low");
				return null;
			}
			Block checkBlock = block.getWorld().getBlock(block.getX(), yscan, block.getZ());
			if (isValidShaftBlock(checkBlock)){
				if (!elevator.chunks.contains(checkBlock.getChunk()))
					elevator.chunks.add(checkBlock.getChunk());
			} else if (SpoutElevatorManager.isBaseBlock(checkBlock)) {
				elevator.baseBlockType = checkBlock.getMaterial();
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
				plugin.logDebug("==Unknown Error==");
				plugin.logDebug("Yscan: " + Integer.toString(yscan));
				plugin.logDebug("Block: " + checkBlock.getMaterial().toString());
				plugin.logDebug("Is Valid Block: " + Boolean.toString(isValidShaftBlock(checkBlock)));
				plugin.logDebug("Is Base Block: " + Boolean.toString(SpoutElevatorManager.isBaseBlock(checkBlock)));
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
		if (checkBlock.getMaterial() == VanillaMaterials.AIR || checkBlock.getMaterial() == plugin.floorBlock
				|| checkBlock.getMaterial() == VanillaMaterials.TORCH || checkBlock.getMaterial() == VanillaMaterials.WALL_SIGN
				|| checkBlock.getMaterial() == VanillaMaterials.STONE_BUTTON || checkBlock.getMaterial() == VanillaMaterials.VINES
				|| checkBlock.getMaterial() == VanillaMaterials.LADDER || checkBlock.getMaterial() == VanillaMaterials.WATER
				|| checkBlock.getMaterial() == VanillaMaterials.STATIONARY_WATER || checkBlock.getMaterial() == VanillaMaterials.WOOD_BUTTON)
			return true;
		return false;
	}
	
	//Recursive function that constructs our list of blocks
	//I'd rather it just return a hashset instead of passing elevator
	//But I can't figure out a clean way to do it
	public static void scanBaseBlocks(Block block, SpoutElevator elevator){
		if (elevator.baseBlocks.size() >= plugin.liftArea)
			return; //5x5 max, prevents infinite loops
		else if (elevator.baseBlocks.contains(block))
			return; // We have that block already
		elevator.baseBlocks.add(block);
		if (block.translate(BlockFace.NORTH, 1).getMaterial() == elevator.baseBlockType)
			scanBaseBlocks(block.translate(BlockFace.NORTH), elevator);
		if (block.translate(BlockFace.EAST, 1).getMaterial() == elevator.baseBlockType)
			scanBaseBlocks(block.translate(BlockFace.EAST), elevator);
		if (block.translate(BlockFace.SOUTH, 1).getMaterial() == elevator.baseBlockType)
			scanBaseBlocks(block.translate(BlockFace.SOUTH), elevator);
		if (block.translate(BlockFace.WEST, 1).getMaterial() == elevator.baseBlockType)
			scanBaseBlocks(block.translate(BlockFace.WEST), elevator);
		return;
	}
	
	public static String constructFloors(SpoutElevator elevator){
		String message = "";

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
				Block testBlock = b.getWorld().getBlock(x, y1, z);
				if (!isValidShaftBlock(testBlock)){
					message += " | " + x + " " + y1 + " " + z + " of type "  + testBlock.getMaterial().toString();
					break;
				}

				if (testBlock.getMaterial() == VanillaMaterials.STONE_BUTTON || testBlock.getMaterial() == VanillaMaterials.WOOD_BUTTON){
					if (plugin.checkGlass)
						if (!scanFloorAtY(currentWorld, testBlock.getY() - 2, elevator)){
							break;
						}
					Floor floor = new Floor();
					floor.setY(y1);
					if (testBlock.translate(BlockFace.BOTTOM).getMaterial() == VanillaMaterials.WALL_SIGN)
						floor.setName(((Sign) testBlock.translate(BlockFace.BOTTOM).getComponent()).getText()[1]);
					if (testBlock.translate(BlockFace.TOP).getMaterial() == VanillaMaterials.WALL_SIGN)
						elevator.floormap.put(y1, floor);
					plugin.logDebug("Floor added: " + testBlock.getPosition().toString());
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
	
	public static boolean scanFloorAtY(World world, int y, SpoutElevator elevator){
		for (Block block : elevator.baseBlocks){
			plugin.logDebug("Scan glass block type: " + world.getBlock(block.getX(), y, block.getZ()).getMaterial().toString());
			plugin.logDebug("Is not glass?: " + Boolean.toString(world.getBlock(block.getX(), y, block.getZ()).getMaterial() != plugin.floorBlock));
			plugin.logDebug("Is not base?: " + Boolean.toString(!plugin.blockSpeeds.keySet().contains(world.getBlock(block.getX(), y, block.getZ()).getMaterial())));
			if (world.getBlock(block.getX(), y, block.getZ()).getMaterial() != plugin.floorBlock && !plugin.blockSpeeds.keySet().contains(world.getBlock(block.getX(), y, block.getZ()).getMaterial())){
				if (BukkitLift.debug)
					System.out.println("Invalid block type");
				return false;	
			}
		}
		return true;
	}
	
	public static void endLift(SpoutElevator elevator){
		plugin.logDebug("Halting lift");
		for (Block b : elevator.glassBlocks)
			b.setMaterial((BlockMaterial) plugin.floorBlock);
		for (Entity p : elevator.passengers){
			fallers.remove(p);
			if (p instanceof Player)
				removePlayer((Player) p);
			else
				elevator.passengers.remove(p);
		}
		Iterator<Entity> passengerIterators = elevator.holders.keySet().iterator();
		while (passengerIterators.hasNext()){
			Entity passenger = passengerIterators.next();
			if (passenger instanceof Player){
				removePlayer((Player) passenger, passengerIterators);
			}
		}
		elevator.clear();
	}
	
	public static void removePlayer(Player player, Iterator<Entity> passengers){
		plugin.logDebug("Removing player " + player.getName() + " from El: " + elevators.toString());
		for (SpoutElevator elevator : elevators){
			plugin.logDebug("Scanning lift");
			if (elevator.passengers.contains(player) || elevator.holders.containsKey(player)){
				plugin.logDebug("Removing player from lift");
				//elevator.passengers.remove(player);
				if (fallers.contains(player)){
					fallers.remove(player);
				}
				if (flyers.contains(player)){
					flyers.remove(player);
				} else {
					player.get(Human.class).setCanFly(false);
					plugin.logDebug("Removing player from flight");			
				}			
				passengers.remove();
			}
		}
	}
	
	public static void removePlayer(Player player){
		plugin.logDebug("Removing player " + player.getName() + " from El: " + elevators.toString());
		for (SpoutElevator elevator : elevators){
			plugin.logDebug("Scanning lift");
			if (elevator.passengers.contains(player) || elevator.holders.containsKey(player)){
				plugin.logDebug("Removing player from lift");
				if (fallers.contains(player)){
					fallers.remove(player);
				}
				if (flyers.contains(player)){
					flyers.remove(player);
				} else {
					player.get(Human.class).setCanFly(false);
					plugin.logDebug("Removing player from flight");
				}
				if (elevator.holders.containsKey(player))
					elevator.holders.remove(player);
				elevator.passengers.remove(player);
			}
		}
	}
	
	public static boolean isBaseBlock(Block block){
		if (plugin.blockSpeeds.containsKey(block.getMaterial()))
			return true;
		return false;
	}
	
	public static boolean isPassenger(Entity entity){
		Iterator<SpoutElevator> iterator = elevators.iterator();
		while (iterator.hasNext()){
			SpoutElevator elevator = iterator.next();
			if (elevator.passengers.contains(entity))
				return true;
		}
		return false;
	}
	
	public void run() {
		//Using while loop iterator so we can remove lifts in a sane way
		Iterator<SpoutElevator> eleviterator = elevators.iterator();
		//for (Elevator e : elevators){
		while (eleviterator.hasNext()){
			SpoutElevator e = eleviterator.next();
			plugin.logDebug("Passengers: " + e.passengers.toString());
			if(e.passengers.isEmpty()){
				SpoutElevatorManager.endLift(e);
				eleviterator.remove();
				continue;
			}
			
			//Re apply impulse as it does seem to run out
			for (Entity p : e.getPassengers()){
				if(e.destFloor.getFloor() > e.startFloor.getFloor()){
					plugin.logDebug("Processing up: " + p.toString());
					//p.getScene().setMovementVelocity(new Vector3(0.0D, e.speed, 0.0D));
					p.getScene().setPosition(p.getScene().getPosition().add(0.0D, 1.0D, 0.0D));
				} else {
					plugin.logDebug("Processing down: " + p.toString());
					p.getScene().setPosition(p.getScene().getPosition().add(0.0D, -e.speed, 0.0D));
					//p.getScene().setMovementVelocity(new Vector3(0.0D, -e.speed, 0.0D));
				}
				//p.setFallDistance(0.0F);
			}
			
			Iterator<Entity> passengers = e.passengers.iterator();
			while (passengers.hasNext()){
				Entity passenger = passengers.next();
				
				//Check if passengers have left the shaft
				if (!e.isInShaft(passenger) && passenger instanceof Player){
					logDebug("Player out of shaft");
					removePlayer((Player) passenger, passengers);
					continue;
				}
				
				if((e.goingUp && passenger.getScene().getPosition().getY() > e.destFloor.getY() - 0.7)
						|| (!e.goingUp && passenger.getScene().getPosition().getY() < e.destFloor.getY()-0.1)){
					logDebug("Removing passenger: " + passenger.toString() + " with y " + Double.toString(passenger.getScene().getPosition().getY()));
					logDebug("Trigger status: Going up: " + Boolean.toString(e.goingUp));
					logDebug("Floor Y: " + Double.toString(e.destFloor.getY()));
					//passenger.getScene().setMovementVelocity(new Vector3(0,0,0));
					//Location pLoc = passenger.getLocation();
					//pLoc.setY(e.destFloor.getY()-0.7);
					//passenger.teleport(pLoc);
					e.holders.put(passenger, passenger.getScene().getPosition());
					if (e instanceof Player)
						removePlayer((Player) passenger, passengers);
					else
						passengers.remove();
				}
			}
			
			for (Entity holder : e.holders.keySet()){
				plugin.logDebug("Holding: " + holder.toString() + " at " + e.holders.get(holder));
				holder.getScene().setPosition(e.holders.get(holder));
				//holder.setFallDistance(0.0F);
			}
		}
	}
	
	private void logDebug(String message){
		plugin.logDebug("[SpoutElevatorManager] " + message);
	}
}
