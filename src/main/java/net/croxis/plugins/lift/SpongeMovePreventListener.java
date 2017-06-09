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

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;

/**
 * Created by croxis on 5/29/17.
 */
public class SpongeMovePreventListener {
    @Listener
    public void onPlayerMove(MoveEntityEvent event){
        for (SpongeElevator elevator : SpongeElevatorManager.elevators){
            World world = event.getToTransform().getExtent();
            Chunk chunk = world.getChunkAtBlock(event.getToTransform().getLocation().getBlockPosition()).get();
            if (elevator.chunks.contains(chunk)){
                if (elevator.isInShaft(event.getTargetEntity())
                        && ! elevator.isInLift(event.getTargetEntity())
                        && SpongeConfig.preventEntry){
                    event.setToTransform(event.getFromTransform());
                    //event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().multiply(-1));
                    if (event.getTargetEntity() instanceof Player){
                        ((Player) event.getTargetEntity()).sendMessage(Text.of(SpongeConfig.stringCantEnter));
                    }
                }
            }
        }
    }
}
