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
	public static boolean debug = false;
	static boolean redstone = true;
	static int liftArea = 16;
	static int maxHeight = 256;
	static boolean autoPlace = false;
	static boolean checkFloor = true;
	static boolean serverFlight = false;
	static boolean liftMobs = false;
	static boolean preventEntry = false;
	static boolean preventLeave = false;
	static boolean mouseScroll = false;
	static String stringDestination = "§1Dest";
	static String stringCurrentFloor = "§4Current Floor";
	static String stringOneFloor;
	static String stringCantEnter;
	static String stringCantLeave;
	static String stringUnsafe;
	static String stringScrollSelectEnabled;
	static String stringScrollSelectDisabled;
    static int signVersion = 2;
}
