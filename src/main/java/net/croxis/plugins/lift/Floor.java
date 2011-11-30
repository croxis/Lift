package net.croxis.plugins.lift;

public class Floor {
	private String name = "";
	private int y;
	private int floor;
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

}
