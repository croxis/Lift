package net.croxis.plugins.lift;

import org.bukkit.World;

public class Floor {
	private String name = "";
	private int y;
	private int floor;
	private World world;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
		if (name == null)
			this.name = "";
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getFloor() {
		return floor;
	}
	public void setFloor(int floor) {
		this.floor = floor;
	}
	public World getWorld() {
		return world;
	}
	public void setWorld(World world) {
		this.world = world;
	}

}
