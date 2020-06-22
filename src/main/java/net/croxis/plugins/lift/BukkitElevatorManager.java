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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

public class BukkitElevatorManager extends ElevatorManager{
    private static BukkitLift plugin;
    static HashSet<BukkitElevator> bukkitElevators = new HashSet<>();
    public static HashSet<Entity> fallers = new HashSet<>();
    public static HashSet<Player> fliers = new HashSet<>();


    public BukkitElevatorManager(BukkitLift plugin) {
        BukkitElevatorManager.plugin = plugin;
        taskid = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 1, 1);
    }

    public static void clear(){
        bukkitElevators = new HashSet<>();
        fallers = new HashSet<>();
        fliers = new HashSet<>();
    }

    public static BukkitElevator createLift(Block block, String cause){
        long startTime = System.currentTimeMillis();
        plugin.logDebug("Starting elevator gen caused by: " + cause + " v" + plugin.getDescription().getVersion());
        BukkitElevator bukkitElevator = new BukkitElevator(plugin);
        bukkitElevator.cause = "Starting elevator gen caused by: " + cause + " v" + plugin.getDescription().getVersion();
        int yscan = block.getY() - 1;
        while(yscan >= -1){
            if (yscan == -1){ //Gone too far with no base abort!
                plugin.logDebug("No elevator base found");
                bukkitElevator.setFailReason("No elevator base found");
                return null;
            }
            Block checkBlock = block.getWorld().getBlockAt(block.getX(), yscan, block.getZ());
            if (isValidShaftBlock(checkBlock)){
                // Do nothing keep going
            } else if (BukkitElevatorManager.isBaseBlock(checkBlock)) {
                bukkitElevator.baseBlockType = checkBlock.getType();
                bukkitElevator.speed = plugin.getBlockSpeed(checkBlock.getType());
                scanBaseBlocks(checkBlock, bukkitElevator);
                for (Block b : bukkitElevator.baseBlocks){
                    // This is for speed optimization for entering lift in use
                    if (!bukkitElevator.chunks.contains(b.getChunk()))
                        bukkitElevator.chunks.add(b.getChunk());
                }
                break;
            } else {
                // Something is obstructing the elevator so stop
                plugin.logDebug("==Unknown Error==");
                plugin.logDebug("Yscan: " + Integer.toString(yscan));
                plugin.logDebug("Block: " + checkBlock.getType().toString());
                plugin.logDebug("Is Valid Block: " + Boolean.toString(isValidShaftBlock(checkBlock)));
                plugin.logDebug("Is Base Block: " + Boolean.toString(BukkitElevatorManager.isBaseBlock(checkBlock)));
                return null;
            }
            yscan--;
        }
        plugin.logDebug("Base size: " + Integer.toString(bukkitElevator.baseBlocks.size()) + " at " + bukkitElevator.baseBlocks.iterator().next().getLocation().toString());

        constructFloors(bukkitElevator);

        //Elevator is constructed, pass off to check signs for floor destination, collect all people and move them
        plugin.logDebug("Elevator gen took: " + (System.currentTimeMillis() - startTime) + " ms.");
        return bukkitElevator;
    }

    //Checks if block is a valid elevator block SANS iron
    public static boolean isValidShaftBlock(Block checkBlock){
        return (BukkitConfig.floorMaterials.contains(checkBlock.getType())
                || checkBlock.isEmpty()
                || !checkBlock.getType().isSolid()
                || checkBlock.getType() == Material.AIR
                || checkBlock.getType() == Material.LADDER
                || checkBlock.getType() == Material.SNOW
                || checkBlock.getType() == Material.STONE_BUTTON
                || checkBlock.getType() == Material.TORCH
                || checkBlock.getType() == Material.VINE
                || checkBlock.getType().toString().matches(".*?WALL_SIGN")
                || checkBlock.getType() == Material.WATER
                || checkBlock.getType() == Material.RAIL
                || checkBlock.getType() == Material.DETECTOR_RAIL
                || checkBlock.getType() == Material.ACTIVATOR_RAIL
                || checkBlock.getType() == Material.POWERED_RAIL
                || checkBlock.getType() == Material.REDSTONE_WIRE);
    }

    //Recursive function that constructs our list of blocks
    //I'd rather it just return a hashset instead of passing elevator
    //But I can't figure out a clean way to do it
    public static void scanBaseBlocks(Block block, BukkitElevator bukkitElevator){
        if (bukkitElevator.baseBlocks.size() >= BukkitConfig.liftArea || bukkitElevator.baseBlocks.contains(block))
            return; //5x5 max, prevents infinite loops
        bukkitElevator.baseBlocks.add(block);
        if (block.getRelative(BlockFace.NORTH, 1).getType() == bukkitElevator.baseBlockType)
            scanBaseBlocks(block.getRelative(BlockFace.NORTH), bukkitElevator);
        if (block.getRelative(BlockFace.EAST, 1).getType() == bukkitElevator.baseBlockType)
            scanBaseBlocks(block.getRelative(BlockFace.EAST), bukkitElevator);
        if (block.getRelative(BlockFace.SOUTH, 1).getType() == bukkitElevator.baseBlockType)
            scanBaseBlocks(block.getRelative(BlockFace.SOUTH), bukkitElevator);
        if (block.getRelative(BlockFace.WEST, 1).getType() == bukkitElevator.baseBlockType)
            scanBaseBlocks(block.getRelative(BlockFace.WEST), bukkitElevator);
    }

    public static boolean isButton(Block testBlock){
        return BukkitConfig.buttonMaterials.contains(testBlock.getType());
    }

    public static boolean isCarpet(Block testBlock){
        return testBlock.getType().toString().contains("CARPET");
    }

    public static String constructFloors(BukkitElevator bukkitElevator){
        StringBuilder message = new StringBuilder();
        //String message = "";
        int y1 = bukkitElevator.baseBlocks.iterator().next().getY();
        int maxY = y1 + BukkitConfig.maxHeight;

        for (Block b : bukkitElevator.baseBlocks){
            int x = b.getX();
            int z = b.getZ();
            y1 = b.getY();
            int scanHeight = 0;

            World currentWorld = b.getWorld();

            while (true){
                y1 = y1 + 1;
                scanHeight += 1;
                if (scanHeight == BukkitConfig.maxHeight + 2 || scanHeight >= maxY) {
                    break;
                }
                Block testBlock = b.getWorld().getBlockAt(x, y1, z);
                if (!isValidShaftBlock(testBlock)){
                    message.append(" | " + x + " " + y1 + " " + z + " of type "  + testBlock.getType().toString());
                    maxY = y1;
                    plugin.logDebug("Not valid shaft block" + x + " " + y1 + " " + z + " of type "  + testBlock.getType().toString());
                    break;
                }
                if (isButton(testBlock)){
                    if (BukkitConfig.checkFloor)
                        if (!scanFloorAtY(currentWorld, testBlock.getY() - 2, bukkitElevator)){
                            break;
                        }
                    BukkitFloor floor = new BukkitFloor(testBlock, y1);

                    // Use old signs first for compatibility.
                    if (testBlock.getRelative(BlockFace.DOWN).getType().toString().matches(".*?WALL_SIGN")){
                        Sign sign = (Sign) testBlock.getRelative(BlockFace.DOWN).getState();
                        if (!sign.getLine(0).isEmpty())
                            floor.setName(sign.getLine(0));
                        else if (!sign.getLine(1).isEmpty())
                            floor.setName(sign.getLine(1));
                    }
                    else if (testBlock.getRelative(BlockFace.UP).getType().toString().matches(".*?WALL_SIGN")){
                        LiftSign liftSign = new LiftSign(BukkitLift.config, ((Sign) testBlock.getRelative(BlockFace.UP).getState()).getLines());
                        floor.setName(liftSign.getCurrentName());
                    }
                    bukkitElevator.floormap.put(y1, floor);
                    plugin.logDebug("Floor added at lift: " + testBlock.getLocation().toString());
                    plugin.logDebug("Floor y: " + Integer.toString(y1));
                }
            }
        }
        int floorNumber = 1;
        Iterator<Integer> floorIterator = bukkitElevator.floormap.keySet().iterator();
        while (floorIterator.hasNext()){
            if (floorIterator.next() >= maxY)
                floorIterator.remove();
        }
        for (Floor floor : bukkitElevator.floormap.values()){
            floor.setFloor(floorNumber);
            bukkitElevator.floormap2.put(floorNumber, floor);
            floorNumber = floorNumber + 1;
        }
        return message.toString();
    }

    public static boolean scanFloorAtY(World world, int y, BukkitElevator bukkitElevator){
        for (Block block : bukkitElevator.baseBlocks){
            plugin.logDebug("Scan floor block type: " + world.getBlockAt(block.getX(), y, block.getZ()).getType().toString());
            if (!BukkitConfig.floorMaterials.contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())
                    && !BukkitConfig.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType())
                    && !(world.getBlockAt(block.getX(), y, block.getZ()).isEmpty())){
                plugin.logDebug("Invalid block type in lift shaft.");
                plugin.logDebug("Is valid flooring?: " + BukkitConfig.floorMaterials.contains(world.getBlockAt(block.getX(), y, block.getZ()).getType()));
                plugin.logDebug("Is base?: " + BukkitConfig.blockSpeeds.keySet().contains(world.getBlockAt(block.getX(), y, block.getZ()).getType()));
                plugin.logDebug("Is air?: " + (world.getBlockAt(block.getX(), y, block.getZ()).getType() == Material.AIR));
                return false;
            }
        }
        return true;
    }

    public static void endLift(BukkitElevator bukkitElevator){
        plugin.logDebug("Halting lift");
        for (BlockState state : bukkitElevator.getFloorBlocks().values()){
            state.update(true);
        }
        for (BlockState state : bukkitElevator.getAboveFloorBlocks().values()){
            state.update(true);
        }

        Iterator<Entity> passengerIterator = bukkitElevator.getPassengers();
        while (passengerIterator.hasNext()){
            Entity e = passengerIterator.next();
            e.setFallDistance(0);
            fallers.remove(e);
            e.setVelocity(new Vector(0, 0, 0));
            if (e instanceof Player)
                removePlayer((Player) e);
            else if (e instanceof Minecart) {
                final Vector v = bukkitElevator.getMinecartSpeeds().get(e);
                e.setVelocity(v != null ? v : new Vector(0, 0, 0));
            }
            if (e instanceof Vehicle){
                List<Entity> vehiclePassengers = e.getPassengers();
                for (Entity vehiclePassenger : vehiclePassengers) {
                    if (vehiclePassenger instanceof Player)
                        restorePlayer((Player) vehiclePassenger);
                }
            }
            e.setGravity(true);
            passengerIterator.remove();
        }
        Iterator<Entity> holdersIterators = bukkitElevator.getHolders();
        while (holdersIterators.hasNext()){
            Entity passenger = holdersIterators.next();
            passenger.setFallDistance(0);
            passenger.setGravity(true);
            if (passenger instanceof Player){
                removePlayer((Player) passenger, holdersIterators);
            } else if (passenger instanceof Minecart) {
                final Vector v = bukkitElevator.getMinecartSpeeds().get(passenger);
                passenger.setVelocity(v != null ? v : new Vector(0, 0, 0));
            }
            if (passenger instanceof Vehicle){
                List<Entity> vehiclePassengers = passenger.getPassengers();
                for (Entity vehiclePassenger : vehiclePassengers) {
                    if (vehiclePassenger instanceof Player)
                        restorePlayer((Player) vehiclePassenger);
                }
            }
        }
        //Fire off redstone signal for arrival
        //org.bukkit.material.Sign sign;
        Block s = ((BukkitFloor) bukkitElevator.destFloor).getButton().getRelative(BlockFace.UP);
        BlockData data = s.getBlockData();
        if (!(data instanceof org.bukkit.block.data.type.WallSign)){
            plugin.logInfo("WARNING: Unable to get sign at destination for redstone pulse.");
            plugin.logInfo("Sign coords: " + s.getLocation().toString());
            plugin.logInfo("Sign material: " + s.getType().toString());
            bukkitElevator.clear();
            plugin.logDebug("Ended lift");
            return;
        }
        //try{
        //	sign = (org.bukkit.material.Sign) s.getState().getData();
        //} catch(Exception exception) {
        //	plugin.logInfo("WARNING: Unable to get sign at destination for redstone pulse.");
        //	plugin.logInfo("Sign coords: " + s.getLocation().toString());
        //	plugin.logInfo("Sign material: " + s.getType().toString());
        //	bukkitElevator.clear();
        //	plugin.logDebug("Ended lift");
        //	return;
        //}
        org.bukkit.block.data.type.WallSign sign = (org.bukkit.block.data.type.WallSign) data;

        BlockFace directionFacing = sign.getFacing();
        if (directionFacing == BlockFace.NORTH){
            Block checkBlock = s.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH);
            BlockData blockData = checkBlock.getBlockData();
            if (blockData instanceof Powerable){
                Powerable powerData = (Powerable) blockData;
                powerData.setPowered(true);
                checkBlock.setBlockData(powerData);
            }
        } else if (directionFacing == BlockFace.EAST){
            Block checkBlock = s.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST);
            BlockData blockData = checkBlock.getBlockData();
            if (blockData instanceof Powerable){
                Powerable powerData = (Powerable) blockData;
                powerData.setPowered(true);
                checkBlock.setBlockData(powerData);
            }
        } else if (directionFacing == BlockFace.SOUTH){
            Block checkBlock = s.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH);
            BlockData blockData = checkBlock.getBlockData();
            if (blockData instanceof Powerable){
                Powerable powerData = (Powerable) blockData;
                powerData.setPowered(true);
                checkBlock.setBlockData(powerData);
            }
        } else if (directionFacing == BlockFace.WEST){
            Block checkBlock = s.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST);
            BlockData blockData = checkBlock.getBlockData();
            if (blockData instanceof Powerable){
                Powerable powerData = (Powerable) blockData;
                powerData.setPowered(true);
                checkBlock.setBlockData(powerData);
            }
        }
        bukkitElevator.clear();
        plugin.logDebug("Ended lift");
    }

    public static void removePlayer(Player player, Iterator<Entity> passengers){
        plugin.logDebug("Removing player " + player.getName() + " from El: " + bukkitElevators.toString());
        for (BukkitElevator bukkitElevator : bukkitElevators){
            plugin.logDebug("Scanning lift");
            if (bukkitElevator.isInLift(player)){
                plugin.logDebug("Removing player from lift");
                player.setFallDistance(0);
                restorePlayer(player);
                passengers.remove();
            }
        }
    }

    public static void removePlayer(Player player){
        plugin.logDebug("Removing player " + player.getName() + " from El: " + bukkitElevators.toString());
        for (BukkitElevator bukkitElevator : bukkitElevators){
            plugin.logDebug("Scanning lift");
            if (bukkitElevator.isInLift(player)){
                plugin.logDebug("Removing player from lift");
                player.setVelocity(new Vector(0, 0, 0));
                player.setFallDistance(0);
                restorePlayer(player);
                bukkitElevator.removePassenger(player);
            }
        }
    }

    public static void removePassenger(Entity passenger){
        if (isInALift(passenger)){
            plugin.logDebug("Removing entity " + passenger.toString() + " from El: " + bukkitElevators.toString());
            passenger.setVelocity(new Vector(0, 0, 0));
            passenger.setFallDistance(0);
            passenger.setGravity(true);
            if (passenger instanceof Player)
                removePlayer((Player) passenger);
            else
                for (BukkitElevator bukkitElevator : bukkitElevators){
                    plugin.logDebug("Scanning lift");
                    if (bukkitElevator.isInLift(passenger))
                        bukkitElevator.removePassenger(passenger);
                }
        }
    }

    public static boolean isBaseBlock(Block block){
        return BukkitConfig.blockSpeeds.containsKey(block.getType());
    }

    public static boolean isInALift(Entity entity){
        for (BukkitElevator bukkitElevator : bukkitElevators) {
            if (bukkitElevator.isInLift(entity))
                return true;
        }
        return false;
    }

    public static void setupPlayer(Player player){
        // Function which sets up a player for holding or passengering. Anti cheat stuff
        plugin.logDebug("[Manager][setupPlayer] " + player.getDisplayName());
        if (player.getAllowFlight()){
            BukkitElevatorManager.fliers.add(player);
            plugin.logDebug(player.getName() + " added to flying list");
        } else {
            BukkitElevatorManager.fliers.remove(player);
            //player.setAllowFlight(false);
            plugin.logDebug(player.getName() + " NOT added to flying list");
        }

        player.setGravity(false);
        player.setAllowFlight(true);

        if (BukkitConfig.useNoCheatPlus){
            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_NOFALL);
            NCPExemptionManager.exemptPermanently(player, CheckType.MOVING_SURVIVALFLY);
        }
    }

    static void restorePlayer(Player player){
        // Restores a player's previous stats.
        plugin.logDebug("[Manager][restorePlayer] " + player.getDisplayName());
        player.setGravity(true);
        if (fallers.contains(player)){
            fallers.remove(player);
        }
        if (fliers.contains(player)){
            fliers.remove(player);
        } else {
            player.setAllowFlight(false);
            plugin.logDebug("Removing player from flight");
            if (BukkitConfig.useNoCheatPlus){
                NCPExemptionManager.unexempt(player, CheckType.MOVING_NOFALL);
                NCPExemptionManager.unexempt(player, CheckType.MOVING_SURVIVALFLY);
            }
        }
    }

    public void run() {
        //Using while loop iterator so we can remove lifts in a sane way
        Iterator<BukkitElevator> eleviterator = bukkitElevators.iterator();
        // Various variables to reduce variable spawning
        BukkitElevator e;
        Iterator<Entity> passengers;
        Entity passenger;
        Entity holder;

        while (eleviterator.hasNext()){
            e = eleviterator.next();
            if (e == null) {
                eleviterator.remove();
                continue;
            }
            plugin.logDebug("Processing elevator: " + e);
            passengers = e.getPassengers();
            if(!passengers.hasNext()){
                BukkitElevatorManager.endLift(e);
                eleviterator.remove();
                continue;
            }

            // If the lift has been running 5 seconds longer than it should of
            // Teleport all players and end the lift
            // Speed is blocks per second or tick (it is unclear)
            if (e.startTime + e.speed * 20 * 1000 + 5000 >= System.currentTimeMillis()) {
                plugin.logDebug("Ending lift due to timeout.");
                e.quickEndLift();
                BukkitElevatorManager.endLift(e);
                eleviterator.remove();
                continue;
            }

            while (passengers.hasNext()){
                passenger = passengers.next();
                if (passenger == null){
                    continue;
                }
                //Check if passengers have left the shaft
                if (!e.isInShaft(passenger)){
                    plugin.logDebug("Player out of shaft");
                    if (BukkitConfig.preventLeave){
                        if (passenger instanceof Player)
                            passenger.sendMessage(BukkitConfig.stringCantLeave);
                        Location baseLoc = e.baseBlocks.iterator().next().getLocation();
                        Location playerLoc = passenger.getLocation();
                        playerLoc.setX(baseLoc.getX() + 0.5D);
                        playerLoc.setZ(baseLoc.getZ() + 0.5D);
                        passenger.teleport(playerLoc, TeleportCause.UNKNOWN);
                    } else {
                        passenger.setVelocity(new Vector(0, 0, 0));
                        if (passenger instanceof Player)
                            removePlayer((Player) passenger, passengers);
                        else
                            removePassenger(passenger);
                        continue;
                    }
                }

                //Re apply impulse as it does seem to run out
                if (!passenger.isInsideVehicle()) {
                    if (e.destFloor.getFloor() > e.startFloor.getFloor())
                        passenger.setVelocity(new Vector(0.0D, e.speed, 0.0D));
                    else
                        passenger.setVelocity(new Vector(0.0D, -e.speed, 0.0D));
                }
                passenger.setFallDistance(0.0F);

                if((e.goingUp && passenger.getLocation().getY() > e.destFloor.getY() - 0.7)
                        || (!e.goingUp && passenger.getLocation().getY() < e.destFloor.getY()-0.6)){
                    plugin.logDebug("Removing passenger: " + passenger.toString() + " with y " + Double.toString(passenger.getLocation().getY()));
                    plugin.logDebug("Passenger Height: " + passenger.getHeight());
                    plugin.logDebug("Upperbound " + (e.destFloor.getY() - 0.7));
                    plugin.logDebug("Lowerbound " + (e.destFloor.getY()-0.6));
                    plugin.logDebug("Trigger status: Going up: " + e.goingUp);
                    plugin.logDebug("Floor Y: " + e.destFloor.getY());
                    passenger.setVelocity(new Vector(0, 0, 0));
                    Location pLoc = passenger.getLocation().clone();
                    pLoc.setY(e.destFloor.getY()-0.5);
                    passenger.teleport(pLoc, TeleportCause.UNKNOWN);

                    moveToHolder(e, passengers, passenger, passenger.getLocation(), "Passenger not in floor.");
                }
            }

            Iterator<Entity> holders = e.getHolders();

            while (holders.hasNext()){
                holder = holders.next();
                if (holder == null) {
                    continue;
                }
                plugin.logDebug("Holding: " + holder.toString() + " at " + e.getHolderPos(holder));
                holder.teleport(e.getHolderPos(holder), TeleportCause.UNKNOWN);
                holder.setFallDistance(0.0F);
                holder.setVelocity(new Vector(0,0,0));
            }
        }
    }

    private void moveToHolder(BukkitElevator e, Iterator<Entity> passengers,
                              Entity passenger, Location location, String reason) {
        passengers.remove();
        e.addHolder(passenger, location, reason);
        passenger.setVelocity(new Vector(0,0,0));
        passenger.setFallDistance(0.0F);
    }

    public static void addHolder(BukkitElevator elevator, Entity holder, Location location, String reason){
        // Adds a new entity to lift to be held in position
        if (holder instanceof Player)
            setupPlayer((Player) holder);
        elevator.addHolder(holder, location, reason);
        if (!elevator.goingUp) {
            BukkitElevatorManager.fallers.add(holder);
        }
    }

    public static void addPassenger(BukkitElevator elevator, Entity passenger){
        // Adds a new entity to lift to be held in position
        plugin.logDebug("[Manager][addPassenger] Adding passenger " + passenger.toString());
        if (passenger instanceof Player)
            setupPlayer((Player) passenger);
        if (passenger instanceof Vehicle){
            List<Entity> vehiclePassengers = passenger.getPassengers();
            for (Entity vehiclePassenger : vehiclePassengers) {
                if (vehiclePassenger instanceof Player)
                    setupPlayer((Player) vehiclePassenger);
            }
        }
        elevator.addPassenger(passenger);
        if (!elevator.goingUp) {
            BukkitElevatorManager.fallers.add(passenger);
        }
        passenger.setGravity(false);
        plugin.logDebug("[Manager][addPassenger] Added passenger " + passenger.toString());
    }

    public static void quickEndLifts(){
        Iterator<BukkitElevator> iterator = bukkitElevators.iterator();
        while (iterator.hasNext()){
            BukkitElevator elevator = iterator.next();
            elevator.quickEndLift();
            iterator.remove();
        }
    }
}
