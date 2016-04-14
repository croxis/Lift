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

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

public class SpongeElevatorManager extends ElevatorManager {

    private Task task;

    public SpongeElevatorManager() {
        //logger = SpongeLift.instance.getLogger();
    }

    public void init(SpongeLift plugin) {
        this.task = Task.builder().intervalTicks(1).execute(this).name("Lift Task").submit(plugin);
    }

    public static SpongeElevator createLift(Location<World> buttonBlock, String cause) {
        long startTime = System.currentTimeMillis();
        SpongeLift.instance.debug("Starting elevator gen caused by: " + cause);
        SpongeElevator elevator = new SpongeElevator();
        elevator.cause = "Starting elevator gen caused by: " + cause;
        int ycursor = buttonBlock.getBlockY() - 2;
        int ycounter = 0;
        while (ycursor >= -1) {
            if (ycursor == -1 | ycounter > SpongeLift.maxHeight) {
                SpongeLift.instance.getLogger().debug("No elevator base found in range.");
                elevator.setFailReason("No elevator base found in range.");
                return elevator;
            }
            Location<World> checkBlock = buttonBlock.getExtent().getLocation(buttonBlock.getX(), ycursor, buttonBlock.getZ());
            if (isValidShaftBlock(checkBlock)) {
                // Do nothing keep going
            } else if (isBaseBlock(checkBlock)) {
                elevator.baseBlockType = checkBlock.getBlockType();
                elevator.speed = SpongeLift.instance.blockSpeeds.get(checkBlock.getBlockType());
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
                SpongeLift.instance.debug("Block: " + checkBlock.getBlockType().toString());
                SpongeLift.instance.debug("Is Valid Block: " + Boolean.toString(isValidShaftBlock(checkBlock)));
                SpongeLift.instance.debug("Is Base Block: " + Boolean.toString(SpongeElevatorManager.isBaseBlock(checkBlock)));
                elevator.setFailReason("Obstruction at y= " + Integer.toString(ycursor));
                return elevator;
            }

            ycursor--;
            ycounter++;
        }
        SpongeLift.instance
                .debug("Base size: " + Integer.toString(elevator.baseBlocks.size()) + " at " + elevator.baseBlocks.iterator().next().toString());
        constructFloors(elevator);
        //Elevator is constructed, pass off to check signs for floor destination, collect all people and move them
        SpongeLift.instance.debug("Elevator gen took: " + (System.currentTimeMillis() - startTime) + " ms.");
        return elevator;
    }

    public static void constructFloors(SpongeElevator elevator) {
        int ycursor = elevator.baseBlocks.iterator().next().getY();
        int maxY = ycursor + SpongeLift.maxHeight;
        String message = "";

        for (Vector3i baseBlock : elevator.baseBlocks) {
            SpongeLift.instance.debug("Scanning baseblock: " + baseBlock.toString());
            int xcursor = baseBlock.getX();
            ycursor = baseBlock.getY();
            int zcursor = baseBlock.getZ();
            //int scanHeight = 0;
            while (ycursor - baseBlock.getY() <= maxY) {
                ycursor++; // Add 1 here to prevent base blocks from being scanned
                Location<World> testBlock = elevator.getWorld().getLocation(xcursor, ycursor, zcursor);
                if (!isValidShaftBlock(testBlock)) {
                    message += " | " + xcursor + " " + ycursor + " " + zcursor + " of type " + testBlock.getBlockType().toString();
                    maxY = ycursor; // Lift can not be higher than this point.
                    SpongeLift.instance.debug("Not valid shaft block" + message);
                    break;
                }
                if (testBlock.getBlockType().getId().contains("button")) {
                    if (SpongeLift.instance.checkFloor) {
                        if (!isValidFloorAtY(testBlock.getExtent(), testBlock.getBlockY(), elevator)) {
                            break;
                        }
                    }
                    SpongeFloor floor = new SpongeFloor(testBlock, testBlock.getBlockY());
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
                    if (testBlock.getRelative(Direction.UP).getBlockType() == BlockTypes.WALL_SIGN) {
                        elevator.floormap.put(ycursor, floor);
                    }
                    SpongeLift.instance.debug("Floor added at lift: " + testBlock.toString());
                    SpongeLift.instance.debug("Floor y: " + Integer.toString(ycursor));

                }
            }
        }
        int floorNumber = 1;
        Iterator<Integer> floorIterator = elevator.floormap.keySet().iterator();
        while (floorIterator.hasNext()) {
            if (floorIterator.next() >= maxY) {
                floorIterator.remove();
            }
        }
        for (Floor floor : elevator.floormap.values()) {
            floor.setFloor(floorNumber);
            elevator.floormap2.put(floorNumber, floor);
            floorNumber = floorNumber + 1;
        }
    }

    public static boolean isValidShaftBlock(Location<World> location) {
        final BlockState state = location.getBlock();
        final BlockType type = state.getType();
        return (SpongeLift.instance.floorMaterials.contains(type)
                //|| checkBlock.isEmpty?
                || type == BlockTypes.AIR
                || type == BlockTypes.LADDER
                || type == BlockTypes.SNOW
                //|| checkBlock.getType() == BlockTypes.STATIONARY_WATER
                || type == BlockTypes.STONE_BUTTON
                || type == BlockTypes.TORCH
                || type == BlockTypes.VINE
                || type == BlockTypes.WALL_SIGN
                || type == BlockTypes.WATER
                || type == BlockTypes.WOODEN_BUTTON
                || type == BlockTypes.CARPET
                || type == BlockTypes.RAIL
                || type == BlockTypes.DETECTOR_RAIL
                || type == BlockTypes.ACTIVATOR_RAIL
                //|| checkBlock.getType() == BlockTypes.POWERED_RAIL
                || type == BlockTypes.REDSTONE_WIRE);

    }

    public static boolean isBaseBlock(Location<World> checkBlock) {
        return SpongeLift.instance.blockSpeeds.containsKey(checkBlock.getBlockType());
    }

    public static void scanBaseBlocks(Location<World> location, SpongeElevator elevator) {
        final World world = location.getExtent();
        final UUID worldId = world.getUniqueId();
        final BlockType block = location.getBlockType();
        final Collection<Vector3i> positions = elevator.baseBlocks;
        final Vector3i blockPosition = location.getBlockPosition();
        if (elevator.baseBlocks.size() >= SpongeLift.maxLiftArea || positions.contains(blockPosition)) {
            return;
        }
        positions.add(blockPosition);
        if (location.getRelative(Direction.NORTH).getBlockType() == elevator.baseBlockType) {
            scanBaseBlocks(location.getRelative(Direction.NORTH), elevator);
        }
        if (location.getRelative(Direction.EAST).getBlockType() == elevator.baseBlockType) {
            scanBaseBlocks(location.getRelative(Direction.EAST), elevator);
        }
        if (location.getRelative(Direction.SOUTH).getBlockType() == elevator.baseBlockType) {
            scanBaseBlocks(location.getRelative(Direction.SOUTH), elevator);
        }
        if (location.getRelative(Direction.WEST).getBlockType() == elevator.baseBlockType) {
            scanBaseBlocks(location.getRelative(Direction.WEST), elevator);
        }
        return;
    }

    public static boolean isValidFloorAtY(World extent, int y, SpongeElevator elevator) {
        for (Vector3i block : elevator.baseBlocks) {
            SpongeLift.instance.debug("Scan floor block type: " + extent.getBlock(block.getX(), y, block.getZ()).getType().toString());
            if (!SpongeLift.instance.floorMaterials.contains(extent.getBlock(block.getX(), y, block.getZ()).getType())
                && !SpongeLift.instance.blockSpeeds.keySet().contains(extent.getBlock(block.getX(), y, block.getZ()).getType())
                && !(extent.getBlock(block.getX(), y, block.getZ()).getType() == BlockTypes.AIR))//Need isEmpty
            {
                SpongeLift.instance.debug("Is not valid flooring?: " + Boolean
                        .toString(SpongeLift.instance.floorMaterials.contains(extent.getBlock(block.getX(), y, block.getZ()).getType())));
                SpongeLift.instance.debug("Is not base?: " + Boolean
                        .toString(SpongeLift.instance.blockSpeeds.keySet().contains(extent.getBlock(block.getX(), y, block.getZ()).getType())));
                SpongeLift.instance
                        .debug("Is not air?: " + Boolean.toString((extent.getBlock(block.getX(), y, block.getZ()).getType() == BlockTypes.AIR)));
                SpongeLift.instance.debug("Invalid block type in lift shaft.");
                return false;
            }
        }
        return true;
    }

}
