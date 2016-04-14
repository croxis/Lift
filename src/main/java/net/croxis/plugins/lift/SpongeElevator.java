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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class SpongeElevator extends Elevator{
	public BlockType baseBlockType = null;
	public UUID worldId;
	public Set<Vector3i> baseBlocks = new HashSet<>();
	
	@Override
	public SpongeFloor getFloorFromN(int n){
		return (SpongeFloor) super.getFloorFromN(n);
	}
	
	@Override
	public SpongeFloor getFloorFromY(int y){
		return (SpongeFloor) super.getFloorFromY(y);
	}
	
	@Override
	public void setFailReason(String failReason) {
		super.setFailReason(failReason);
		SpongeLift.instance.getLogger().debug(failReason);
	}

    public World getWorld() {
        return Sponge.getServer().getWorld(this.worldId)
                .orElseThrow(() -> new IllegalStateException("Could not find a world by the uuid: " + this.worldId));
    }
}
