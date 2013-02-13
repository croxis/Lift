package net.croxis.plugins.lift;

import java.util.TreeMap;

public abstract class Elevator {
	public TreeMap <Integer, Floor> floormap = new TreeMap<Integer, Floor>();//Index is y value
	public TreeMap <Integer, Floor> floormap2 = new TreeMap<Integer, Floor>();//Index is floor value
	public int destinationY = 0;//Destination y coordinate
	public Floor destFloor = null;
	public Floor startFloor = null;
	public boolean goingUp = false;
	public double speed = 0.5;

}
