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

public class Config {
	public static boolean debug = true;
	public static boolean redstone = true;
	public static int liftArea = 16;
	public static int maxHeight = 256;
	public static boolean autoPlace = false;
	public static boolean checkFloor = false;
	public static boolean serverFlight = false;
	public static boolean liftMobs = false;
	public static boolean preventEntry = false;
	public static boolean preventLeave = false;
	public static String stringDestination = "§1Dest";
	public static String stringCurrentFloor = "§4Current Floor";
	public static String stringOneFloor;
	public static String stringCantEnter;
	public static String stringCantLeave;
}
