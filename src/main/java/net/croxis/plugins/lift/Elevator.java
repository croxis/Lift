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

import java.util.TreeMap;

public abstract class Elevator {

	//int destinationY = 0; //Destination y coordinate
	String id = "";
	public boolean goingUp = false;
	public double speed;
	private String failReason = "";
	public String cause = "";
	long startTime;
	long maxEndTime;
	boolean cacheLock = false;

	TreeMap <Integer, Floor> floormap = new TreeMap<>();//Index is y value
	TreeMap <Integer, Floor> floormap2 = new TreeMap<>();//Index is floor value

    Floor destFloor = null;
    Floor startFloor = null;

    double centerX;
    double centerZ;

	protected void start() {
    	if (destFloor == null || startFloor == null) {
    		throw new IllegalStateException("Cannot start Elevator with undefined start or destination");
	    }
    	startTime = System.currentTimeMillis();
    	// [|startY-destY| = distance] * [avg sec/block with speed 1 = 0.05] * [to millis = 1000] / speed
		long rideDuration = (long) (Math.abs(startFloor.getY() - destFloor.getY()) * 0.05 * 1000L / speed);
		maxEndTime = startTime + rideDuration + Config.secondsUntilTimeout * 1000;

		calcCenter();
    }

	public void clear(){
		floormap.clear();
		floormap2.clear();
	}

	public Floor getFloorFromN(int n){
		return floormap2.get(n);
	}
	
	public Floor getFloorFromY(int y){
		return floormap.get(y);
	}
	
	public int getTotalFloors(){
		return floormap2.size();
	}

	public String getFailReason() {
		return failReason;
	}
	public void setFailReason(String failReason) {
		this.failReason = failReason;
	}
	
	public String toString() { return "Elevator[" + this.id.toString() + "]";}

	public void setCacheLock(boolean cacheLock) {
		this.cacheLock = cacheLock;
	}

	protected abstract void calcCenter();
}
