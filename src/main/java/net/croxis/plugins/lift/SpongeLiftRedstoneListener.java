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
import java.util.Iterator;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;


public class SpongeLiftRedstoneListener{
    private SpongeLift plugin;

    SpongeLiftRedstoneListener(SpongeLift plugin){
        this.plugin = plugin;
    }
	
	@Listener
    //TODO: Listen for redstone changing instead?
	public void onChangeBlockEventModify(ChangeBlockEvent.Modify event) {
        boolean canDo = false;
        String reason = "";
        SpongeElevator elevator;
        BlockSnapshot snapshot = null;
        BlockState block;
        BlockType blockType;

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            snapshot = transaction.getFinal();
            block = snapshot.getExtendedState();
            blockType = block.getType();
            if ((blockType.equals(BlockTypes.STONE_BUTTON) || blockType.equals(BlockTypes.WOODEN_BUTTON))
                    && block.get(Keys.POWERED).isPresent()
                    && block.get(Keys.POWERED).get()
                    && snapshot.getLocation().get().getRelative(Direction.UP).getBlock().getType().equals(BlockTypes.WALL_SIGN)) {
                canDo = true;
                reason = "Button Press";
                break;
            }
            //https://docs.spongepowered.org/stable/en/plugin/blocks/accessing.html
        }

        if (!canDo) {
            return;
        }
        long startTime = System.currentTimeMillis();
        elevator = SpongeElevatorManager.createLift(snapshot.getLocation().get(), reason);
        if (elevator == null) {
            plugin.debug("Redstone elevator generator returned a null object.");
            plugin.debug("Button block at: " + snapshot.getLocation().get().toString());
            plugin.debug("Please see previous messages to determine why.");
            return;
        }
        TileEntity signEntity = snapshot.getLocation().get().getRelative(Direction.UP).getTileEntity().get();
        Sign sign = (Sign) signEntity;

        LiftSign liftSign = new LiftSign(SpongeLift.config,
                sign.getSignData().get(0).get().toPlain(),
                sign.getSignData().get(1).get().toPlain(),
                sign.getSignData().get(2).get().toPlain(),
                sign.getSignData().get(3).get().toPlain());
        int destination = liftSign.getDestinationFloor();
        if (destination == 0){
            plugin.debug("Not a valid lift sign:" + liftSign.getDebug());
            return;
        }

        //See if lift is in use
        for (SpongeElevator e : SpongeElevatorManager.elevators) {
            Iterator<Location<World>> iterator = elevator.baseBlocks.iterator();
            Iterator<Location<World>> i = e.baseBlocks.iterator();
            while (iterator.hasNext())
                while (i.hasNext())
                    if (i.next().getPosition().sub(iterator.next().getPosition()) == new Vector3d(0, 0, 0)) {
                        plugin.debug("Lift is in use.");
                        return;
                    }
        }

        if (elevator.getTotalFloors() < 2) {
            plugin.debug("Only one floor.");
            return;
        }

        int y = snapshot.getLocation().get().getBlockY();
        elevator.startFloor = elevator.getFloorFromY(y);
        elevator.destFloor = elevator.getFloorFromN(destination);
        
        if (elevator.startFloor == null || elevator.destFloor == null) {
            plugin.getLogger().warn("Critical Error. Startfloor||DestFloor is null. Please report entire stacktrace plus the following error codes.");
            plugin.getLogger().warn("Sign destination: " + Integer.toString(destination));
            plugin.getLogger().warn("Floormap: " + elevator.floormap.toString());
            plugin.getLogger().warn("Floormap2: " + elevator.floormap2.toString());
            plugin.getLogger().warn("Start y: " + Integer.toString(y));
            return;
        }
        
        if (elevator.destFloor.getY() > elevator.startFloor.getY())
            elevator.goingUp = true;
        else
            elevator.goingUp = false;
        
        plugin.debug("Elevator start floor:" + elevator.startFloor.getFloor());
        plugin.debug("Elevator start floor y:" + elevator.startFloor.getY());
        plugin.debug("Elevator destination floor:" + destination);
        plugin.debug("Elevator destination y:" + elevator.destFloor.getY());

        plugin.debug("Floormap: " + elevator.floormap.toString());
        plugin.debug("Floormap: " + elevator.floormap2.toString());

        Iterator<Location<World>> baseBlockIterator = elevator.baseBlocks.iterator();
        for (Chunk chunk : elevator.chunks){
            plugin.debug("Number of entities in this chunk: " + Integer.toString(chunk.getEntities().size()));
            for(Entity entity : chunk.getEntities()){
                if (!SpongeConfig.liftMobs && !(entity instanceof Player))
                    continue;
                if (elevator.isInShaftAtFloor(entity, elevator.startFloor)){
                    if (SpongeElevatorManager.isPassenger(entity)){
                        if (entity instanceof Player)
                            ((Player) entity).sendMessage(Text.of("You are already in a lift. Relog in case of error."));
                        continue;
                    }
                    //Disabling minecart functionaility for now
                    /*if (entity instanceof Minecart) {
                        bukkitElevator.addMinecartSpeed((Minecart) entity);
							//A minecart wont go up if attached to a rail, so we temp remove the rail.
							if (bukkitElevator.goingUp
									&& (entity.getLocation().getBlock().getType() == Material.RAILS
									|| entity.getLocation().getBlock().getType() == Material.DETECTOR_RAIL
									|| entity.getLocation().getBlock().getType() == Material.ACTIVATOR_RAIL
									|| entity.getLocation().getBlock().getType() == Material.POWERED_RAIL)){
								new BukkitRestoreRailTask(entity.getLocation().getBlock()).runTaskLater(plugin, 10);
								entity.getLocation().getBlock().setType(Material.AIR);
							}
							plugin.logDebug("Minecart added to lift");
                    }*/
                    plugin.debug("Adding passenger " + entity.toString());
                    SpongeElevatorManager.addPassenger(elevator, entity);
                    plugin.debug("Added passenger " + entity.toString());
                    if (baseBlockIterator.hasNext() && SpongeConfig.autoPlace){
                        Vector3d loc = baseBlockIterator.next().getPosition();
                        entity.setLocation(new Location<World>(entity.getWorld(), loc.getX() + 0.5D, entity.getLocation().getY(), loc.getZ() + 0.5D));
                    }
                    if (entity instanceof Player){
                        Player playerPassenger = (Player) entity;
                        if (!playerPassenger.hasPermission("lift"))
                            SpongeElevatorManager.addHolder(elevator, entity, entity.getLocation());
                    }
                } else if (!elevator.isInShaftAtFloor(entity, elevator.startFloor) && elevator.isInShaft(entity))
                    SpongeElevatorManager.addHolder(elevator, entity, entity.getLocation());
            }
        }

        //Disable all glass inbetween passengers and destination
        ArrayList<Floor> glassFloors = new ArrayList<>();
        // Going up
        if (elevator.goingUp)
            for(int i = elevator.startFloor.getFloor() + 1; i <= elevator.destFloor.getFloor(); i++){
                glassFloors.add(elevator.floormap2.get(i));
            }
        // Going Down
        else
            for(int i = elevator.destFloor.getFloor() + 1; i <= elevator.startFloor.getFloor(); i++){
                glassFloors.add(elevator.floormap2.get(i));
            }
        for (Floor f : glassFloors){
            for (Location<World> blockLocation : elevator.baseBlocks){
                Location<World> floorBlockLocation = blockLocation.getExtent().getLocation(blockLocation.getBlockX(), f.getY()-2, blockLocation.getBlockZ());
                elevator.addFloorBlock(floorBlockLocation);

                if (floorBlockLocation.getRelative(Direction.UP).getBlockType() == BlockTypes.CARPET){
                    elevator.addCarpetBlock(floorBlockLocation.getRelative(Direction.UP));
                    //floorBlockLocation.getRelative(Direction.UP).setBlockType(BlockTypes.AIR, Cause.source(plugin).build());
                } else if (floorBlockLocation.getRelative(Direction.UP).getBlockType() == BlockTypes.RAIL
                        || floorBlockLocation.getRelative(Direction.UP).getBlockType() == BlockTypes.ACTIVATOR_RAIL
                        || floorBlockLocation.getRelative(Direction.UP).getBlockType() == BlockTypes.DETECTOR_RAIL
                        || floorBlockLocation.getRelative(Direction.UP).getBlockType() == BlockTypes.GOLDEN_RAIL) {
                    //bukkitElevator.addRailBlock(gb.getRelative(BlockFace.UP));
                    //gb.getRelative(BlockFace.UP).setType(Material.AIR);
                } else if (floorBlockLocation.getRelative(Direction.UP).getBlockType() == BlockTypes.REDSTONE_WIRE) {
                    elevator.addRedstoneBlock(floorBlockLocation.getRelative(Direction.UP));
                    //floorBlockLocation.getRelative(Direction.UP).setBlockType(BlockTypes.AIR, Cause.source(plugin).build());
                }
                //floorBlockLocation.setBlockType(BlockTypes.AIR, Cause.source(plugin.container).build());
            }

            SpongeElevatorManager.elevators.add(elevator);

            plugin.debug("Going Up: " + Boolean.toString(elevator.goingUp));
            plugin.debug("Number of passengers: " + Integer.toString(elevator.getSize()));
            plugin.debug("Elevator chunks: " + Integer.toString(elevator.chunks.size()));
            plugin.debug("Total generation time: " + Long.toString(System.currentTimeMillis() - startTime));
        }
    }
}