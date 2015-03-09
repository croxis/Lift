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

import java.util.UUID;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockLoc;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.service.scheduler.Task;
import org.spongepowered.api.util.Direction;

import com.google.inject.Inject;

public class SpongeElevatorManager extends ElevatorManager{
	@Inject
	private Game game;
	private static Logger logger;
	private Task task;
	private UUID uuid;
	public SpongeElevatorManager(){
		logger = SpongeLift.instance.getLogger();
		task = game.getSyncScheduler().runRepeatingTask(SpongeLift.instance, this, 1).get();
		uuid = task.getUniqueId();
	}
	
	public static SpongeElevator createLift(BlockLoc buttonBlock, String cause){
		long startTime = System.currentTimeMillis();
		logger.debug("Starting elevator gen caused by: " + cause + " v");
		SpongeElevator elevator = new SpongeElevator();
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
				// DO nothing keep going
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
				logger.debug("==Obstruction Error==");
				logger.debug("Yscan: " + Integer.toString(ycursor));
				logger.debug("Block: " + checkBlock.getType().toString());
				logger.debug("Is Valid Block: " + Boolean.toString(isValidShaftBlock(checkBlock)));
				logger.debug("Is Base Block: " + Boolean.toString(SpongeElevatorManager.isBaseBlock(checkBlock)));
				elevator.setFailReason("Obstruction at y= " + Integer.toString(ycursor));
				return elevator;
			}
			ycursor--;
			ycounter++;
		}
		logger.debug("Base size: " + Integer.toString(elevator.baseBlocks.size()) + " at " + elevator.baseBlocks.iterator().next().getLocation().toString());
		return elevator;
	}
	
	public static void constructFloors(SpongeElevator elevator){
		int ycursor = elevator.baseBlocks.iterator().next().getY();
		int maxY = ycursor + SpongeLift.maxHeight;
		String message = "";
		
		for (BlockLoc baseBlock: elevator.baseBlocks){
			int xcursor = baseBlock.getX();
			ycursor = baseBlock.getY();
			int zcursor = baseBlock.getZ();
			//int scanHeight = 0;
			
			while (ycursor - baseBlock.getY() <= maxY){
				BlockLoc testBlock = baseBlock.getExtent().getFullBlock(xcursor, ycursor, zcursor);
				if (!isValidShaftBlock(testBlock)){
					message += " | " + xcursor + " " + ycursor + " " + zcursor + " of type "  + testBlock.getType().toString();
					maxY = ycursor; // Lift can not be higher than this point. 
					logger.debug("Not valid shaft block" + message);
					break;
				}
				if (testBlock.getType().getId().contains("button")){
					if (SpongeLift.instance.checkFloor){
						
					}
				}
			}
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

}
