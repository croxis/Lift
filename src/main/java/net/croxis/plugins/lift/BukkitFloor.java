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

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitFloor extends Floor{

	public BukkitFloor(final Block b, final int y){
		super(b.getWorld().getUID(), b.getX(), b.getY(), b.getZ());
	}

	public World getWorld() {
		return Bukkit.getWorld(worldID);
	}
	public void setWorld(World world) {
		worldID = world.getUID();
	}
	Block getButton() {
		return Bukkit.getWorld(worldID).getBlockAt(buttonX, buttonY, buttonZ);
	}
	public void setButton(Block button) {
		buttonX = button.getX();
        buttonY = button.getY();
        buttonZ = button.getZ();
	}
}
