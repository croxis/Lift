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

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockLoc;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.data.Sign;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.service.scheduler.Task;
import org.spongepowered.api.text.message.Message;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.extent.Extent;

import com.google.inject.Inject;

public class SpongeElevatorManager extends ElevatorManager{
	static private Game game;
	@Inject
	private static Logger logger;
	private Task task;
	private UUID uuid;
	public SpongeElevatorManager(Game game){
		//logger = SpongeLift.instance.getLogger();
		this.game = game;
	}
	
	public void init(){
		PluginManager pluginManager = game.getPluginManager();
		PluginContainer liftPlugin = pluginManager.getPlugin("Lift").get();
		task = game.getSyncScheduler().runRepeatingTask(liftPlugin, this, 1).get();
		uuid = task.getUniqueId();
		logger = SpongeLift.instance.getLogger();
	}
	
	public static SpongeElevator createLift(BlockLoc buttonBlock, String cause){
		long startTime = System.currentTimeMillis();
		SpongeLift.instance.debug("Starting elevator gen caused by: " + cause);
		SpongeElevator elevator = new SpongeElevator();
		elevator.cause = "Starting elevator gen caused by: " + cause;
		int ycursor = buttonBlock.getY() - 2;
		int ycounter = 0;
		while(ycursor >= -1){
			if(ycursor == -1 | ycounter > SpongeLift.maxHeight){
				SpongeLift.instance.getLogger().debug("No elevator base found in range.");
				elevator.setFailReason("No elevator base found in range.");
				return elevator;
			}
			BlockLoc checkBlock = buttonBlock.getExtent().getFullBlock(buttonBlock.getX(), ycursor, buttonBlock.getZ());
			if (isValidShaftBlock(checkBlock)){
				// Do nothing keep going
			} else if (isBaseBlock(checkBlock)){
				elevator.baseBlockType = checkBlock.getType();
				elevator.speed = SpongeLift.instance.blockSpeeds.get(checkBlock.getType());
				scanBaseBlocks(checkBlock, elevator);
				//for (BlockLoc b: elevator.baseBlocks){
				// This is for speed optimization for entering lift in use
				// Disabled for now until an equivlent is derived
				//	if (!bukkitElevator.chunks.contains(b.getChunk()))
				//		bukkitElevator.chunks.add(b.getChunk());
				//}
				break;
			} else {
				//Something went wrong
				SpongeLift.instance.debug("==Obstruction Error==");
				SpongeLift.instance.debug("Yscan: " + Integer.toString(ycursor));
				SpongeLift.instance.debug("Block: " + checkBlock.getType().toString());
				SpongeLift.instance.debug("Is Valid Block: " + Boolean.toString(isValidShaftBlock(checkBlock)));
				SpongeLift.instance.debug("Is Base Block: " + Boolean.toString(SpongeElevatorManager.isBaseBlock(checkBlock)));
				elevator.setFailReason("Obstruction at y= " + Integer.toString(ycursor));
				return elevator;
			}
			
			ycursor--;
			ycounter++;
		}
		SpongeLift.instance.debug("Base size: " + Integer.toString(elevator.baseBlocks.size()) + " at " + elevator.baseBlocks.iterator().next().getLocation().toString());
		constructFloors(elevator);
		//Elevator is constructed, pass off to check signs for floor destination, collect all people and move them
		SpongeLift.instance.debug("Elevator gen took: " + (System.currentTimeMillis() - startTime) + " ms.");
		return elevator;
	}
	
	public static void constructFloors(SpongeElevator elevator){
		int ycursor = elevator.baseBlocks.iterator().next().getY();
		int maxY = ycursor + SpongeLift.maxHeight;
		String message = "";
		
		for (BlockLoc baseBlock: elevator.baseBlocks){
			SpongeLift.instance.debug("Scanning baseblock: " + baseBlock.toString());
			int xcursor = baseBlock.getX();
			ycursor = baseBlock.getY(); 
			int zcursor = baseBlock.getZ();
			//int scanHeight = 0;
			while (ycursor - baseBlock.getY() <= maxY){
				ycursor++; // Add 1 here to prevent base blocks from being scanned
				BlockLoc testBlock = baseBlock.getExtent().getFullBlock(xcursor, ycursor, zcursor);
				if (!isValidShaftBlock(testBlock)){
					message += " | " + xcursor + " " + ycursor + " " + zcursor + " of type "  + testBlock.getType().toString();
					maxY = ycursor; // Lift can not be higher than this point. 
					SpongeLift.instance.debug("Not valid shaft block" + message);
					break;
				}
				if (testBlock.getType().getId().contains("button")){
					if (SpongeLift.instance.checkFloor)
						if (!isValidFloorAtY(testBlock.getExtent(), testBlock.getY(), elevator))
							break;
					SpongeFloor floor = new SpongeFloor(testBlock, testBlock.getY());
					/*if (testBlock.getRelative(Direction.DOWN).getType() == BlockTypes.WALL_SIGN){
						BlockLoc downBlock = testBlock.getRelative(Direction.DOWN);
						Sign signBlock = downBlock.getData(Sign.class).get();
						SpongeLift.instance.debug("Sign: " + signBlock.toString());
						Message line = signBlock.getLine(1);
						if (line == null)
							SpongeLift.instance.debug("line is null");
						
						String name = line.toString();
						floor.setName(name);
						
						//And the full line
						floor.setName(testBlock.getRelative(Direction.DOWN).getData(Sign.class).get().getLine(1).toString());
					}*/
					if (testBlock.getRelative(Direction.UP).getType() == BlockTypes.WALL_SIGN)
						elevator.floormap.put(ycursor, floor);
					SpongeLift.instance.debug("Floor added at lift: " + testBlock.getLocation().toString());
					SpongeLift.instance.debug("Floor y: " + Integer.toString(ycursor));
					
				}
			}
		}
		int floorNumber = 1;
		Iterator<Integer> floorIterator = elevator.floormap.keySet().iterator();
		while (floorIterator.hasNext()){
			if (floorIterator.next() >= maxY)
				floorIterator.remove();
		}
		for (Floor floor : elevator.floormap.values()){
			floor.setFloor(floorNumber);
			elevator.floormap2.put(floorNumber, floor);
			floorNumber = floorNumber + 1;
		}
	}
	
	public static boolean isValidShaftBlock(BlockLoc checkBlock){
		return (SpongeLift.instance.floorMaterials.contains(checkBlock.getType())
				//|| checkBlock.isEmpty?
				|| checkBlock.getType() == BlockTypes.AIR
				|| checkBlock.getType() == BlockTypes.LADDER
				|| checkBlock.getType() == BlockTypes.SNOW
				//|| checkBlock.getType() == BlockTypes.STATIONARY_WATER
				|| checkBlock.getType() == BlockTypes.STONE_BUTTON
				|| checkBlock.getType() == BlockTypes.TORCH 
				|| checkBlock.getType() == BlockTypes.VINE 
				|| checkBlock.getType() == BlockTypes.WALL_SIGN
				|| checkBlock.getType() == BlockTypes.WATER
				|| checkBlock.getType() == BlockTypes.WOODEN_BUTTON
				|| checkBlock.getType() == BlockTypes.CARPET
				|| checkBlock.getType() == BlockTypes.RAIL
				|| checkBlock.getType() == BlockTypes.DETECTOR_RAIL
				|| checkBlock.getType() == BlockTypes.ACTIVATOR_RAIL
				//|| checkBlock.getType() == BlockTypes.POWERED_RAIL
				|| checkBlock.getType() == BlockTypes.REDSTONE_WIRE);
		
	}
	
	public static boolean isBaseBlock(BlockLoc checkBlock){
		return SpongeLift.instance.blockSpeeds.containsKey(checkBlock.getType());
	}
	
	public static void scanBaseBlocks(BlockLoc block, SpongeElevator elevator){
		if (elevator.baseBlocks.size() >= SpongeLift.maxLiftArea || elevator.baseBlocks.contains(block))
			return;
		elevator.baseBlocks.add(block);
		if (block.getRelative(Direction.NORTH).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.NORTH), elevator);
		if (block.getRelative(Direction.EAST).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.EAST), elevator);
		if (block.getRelative(Direction.SOUTH).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.SOUTH), elevator);
		if (block.getRelative(Direction.WEST).getType() == elevator.baseBlockType)
			scanBaseBlocks(block.getRelative(Direction.WEST), elevator);
		return;
	}
	
	public static boolean isValidFloorAtY(Extent extent, int y, SpongeElevator elevator){
		for(BlockLoc block: elevator.baseBlocks){
			SpongeLift.instance.debug("Scan floor block type: " + extent.getBlock(block.getX(), y, block.getZ()).getType().toString() );
			if (!SpongeLift.instance.floorMaterials.contains(extent.getBlock(block.getX(), y, block.getZ()).getType())
					&& !SpongeLift.instance.blockSpeeds.keySet().contains(extent.getBlock(block.getX(), y, block.getZ()).getType())
					&& !(extent.getFullBlock(block.getX(), y, block.getZ()).getType() == BlockTypes.AIR))//Need isEmpty
			{
				SpongeLift.instance.debug("Is not valid flooring?: " + Boolean.toString(SpongeLift.instance.floorMaterials.contains(extent.getBlock(block.getX(), y, block.getZ()).getType())));
				SpongeLift.instance.debug("Is not base?: " + Boolean.toString(SpongeLift.instance.blockSpeeds.keySet().contains(extent.getBlock(block.getX(), y, block.getZ()).getType())));
				SpongeLift.instance.debug("Is not air?: " + Boolean.toString((extent.getFullBlock(block.getX(), y, block.getZ()).getType() == BlockTypes.AIR)));
				SpongeLift.instance.debug("Invalid block type in lift shaft.");
				return false;
			}
		}
		return true;
	}

}