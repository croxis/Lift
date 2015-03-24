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

import org.bukkit.World;
import org.bukkit.block.Block;

public class BukkitFloor extends Floor{
	private World world;
	private Block button;

	public BukkitFloor(final Block b, final int y){
		super(y);
		button = b;
	}

	public World getWorld() {
		return world;
	}
	public void setWorld(World world) {
		this.world = world;
	}
	public Block getButton() {
		return button;
	}
	public void setButton(Block button) {
		this.button = button;
	}
}
