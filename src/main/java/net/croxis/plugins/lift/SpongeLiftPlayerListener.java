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

import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.KickPlayerEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SpongeLiftPlayerListener{
    private SpongeLift plugin;
    private Location<World> buttonBlock = null;

    SpongeLiftPlayerListener(SpongeLift plugin){
		this.plugin = plugin;
	}

    @Listener
    public void onPlayerInteract(InteractBlockEvent event, @Root Player player){
        plugin.debug("Start click");
	    if (!event.getTargetBlock().getLocation().isPresent()){
	        return;}
        if (!player.hasPermission("lift.change")){
            return;
        }

        if (!(event.getTargetBlock().getLocation().get().getBlockType() == BlockTypes.WALL_SIGN))
            return;
        plugin.debug("Sign click by: " + player.getName());
	    Location<World> signLoc = event.getTargetBlock().getLocation().get();
	    Location<World> buttonLoc = signLoc.getRelative(Direction.DOWN);
	    if (!(buttonLoc.getBlockType() == BlockTypes.WOODEN_BUTTON || buttonLoc.getBlockType() == BlockTypes.STONE_BUTTON))
	        return;
        long startTime = System.currentTimeMillis();
        SpongeElevator elevator = SpongeElevatorManager.createLift(buttonLoc, "Sign click");
        if (elevator == null){
            plugin.getLogger().warn("Sign elevator gen returned a null object.");
            plugin.getLogger().warn("Button block at: " + buttonLoc.toString());
            return;
        }

        if (elevator.getTotalFloors() < 1)
            // Just a button and sign. Not an elevator.
            return;
        else if (elevator.getTotalFloors() == 1) {
            player.sendMessage(Text.of(SpongeConfig.stringOneFloor));
            return;
        }
        event.setCancelled(true);

        int currentDestinationInt = 1;
        Floor currentFloor = elevator.getFloorFromY(buttonLoc.getBlockY());
        if (currentFloor == null){
            player.sendMessage(Text.of("Elevator generator says this floor does not exist. Check shaft for blockage"));
            return;
        }

        TileEntity entity = signLoc.getTileEntity().get();
		if (entity.supports(SignData.class)){
		    SignData sign = entity.getOrCreate(SignData.class).get();
            plugin.debug("Creating sign: " +  sign.lines().get(0).toPlain());
            plugin.debug("Creating sign: " +  sign.lines().get(1).toPlain());
            plugin.debug("Creating sign: " +  sign.lines().get(2).toPlain());
            plugin.debug("Creating sign: " +  sign.lines().get(3).toPlain());
            LiftSign liftSign = new LiftSign(SpongeLift.config, sign.lines().get(0).toPlain(), sign.lines().get(1).toPlain(), sign.lines().get(2).toPlain(), sign.lines().get(3).toPlain());
            plugin.debug("Sign version: " + Integer.toString(liftSign.signVersion));

            liftSign.setCurrentFloor(currentFloor.getFloor());

            if (signLoc.getRelative(Direction.DOWN).getRelative(Direction.DOWN).getBlockType().equals(BlockTypes.WALL_SIGN)){
                TileEntity nameEntity = signLoc.getRelative(Direction.DOWN).getRelative(Direction.DOWN).getTileEntity().get();
                SignData nameSign = nameEntity.getOrCreate(SignData.class).get();
                Text currentName = nameSign.get(0).get();
                if (currentName.isEmpty())
                    currentName = nameSign.get(1).get();
                liftSign.setCurrentName(currentName.toPlain());
            }

            currentDestinationInt = liftSign.getDestinationFloor();
            currentDestinationInt++;
            if (currentDestinationInt == currentFloor.getFloor())
                currentDestinationInt++;
            // The following line MAY be what causes a potential bug for max floors
            if (currentDestinationInt > elevator.getTotalFloors()) {
                currentDestinationInt = 1;
                if (currentFloor.getFloor() == 1)
                    currentDestinationInt = 2;
            }
            liftSign.setDestinationFloor(currentDestinationInt);
            liftSign.setDestinationName(elevator.getFloorFromN(currentDestinationInt).getName());

            plugin.debug("Sign text: " + liftSign.getDump()[0]);
            plugin.debug("Sign text: " + liftSign.getDump()[1]);
            plugin.debug("Sign text: " + liftSign.getDump()[2]);
            plugin.debug("Sign text: " + liftSign.getDump()[3]);
		    sign.set(sign.lines().set(0, Text.of(liftSign.getDump()[0])));
            sign.set(sign.lines().set(1, Text.of(liftSign.getDump()[1])));
            sign.set(sign.lines().set(2, Text.of(liftSign.getDump()[2])));
            sign.set(sign.lines().set(3, Text.of(liftSign.getDump()[3])));
            entity.offer(sign);
        }

        //SpongeLift.debug("Completed sign update");

	}
	
	@Listener
	public void onPlayerItemPickup(ChangeInventoryEvent.Pickup event){
		//if (SpongeElevatorManager.isPassenger(event.getTargetEntity()))
		//	SpongeElevatorManager.removePassenger(event.getTargetEntity());
	}
	
	@Listener
	public void onEntityDamage(DamageEntityEvent event, @Root DamageSource source){
		if(source.getType().equals(DamageTypes.FALL)){
			Entity faller = event.getTargetEntity();
			if (SpongeElevatorManager.fallers.contains(faller)){
			    event.setCancelled(true);
			    SpongeElevatorManager.fallers.remove(faller);
            }
		}
	}

	@Listener
	public void onPlayerQuit(ClientConnectionEvent.Disconnect event){ SpongeElevatorManager.removePlayer(event.getTargetEntity()); }
	
	@Listener
	public void onPlayerKick(KickPlayerEvent event){
		SpongeElevatorManager.removePlayer(event.getTargetEntity());
	}

}
