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

/**
 * Sign Formats
 * Version 1
 * Line 0: "Current floor" string
 * Line 1: Current floor int only
 * Line 2: "Dest Floor" and int
 * Line 3: Dest floor name string
 *
 * Version 2
 * Line 0: "Current floor" string and int
 * Line 1: Current floor name
 * Line 2: "Dest Floor" and int
 * Line 3: Dest floor name string
 */


/**
 * Created by croxis on 4/28/17.
 */
class LiftSign {
    int signVersion = 0; // 0=hmod, 1=lift till 55, 2=lift>=56
    Config config;
    private String sign0 = ": 0";
    private String sign1 = "";
    private String sign2 = ": 0";
    private String sign3 = "";
    private int currentFloor = 0;
    private int destFloor = 0;
    private String currentName = "";
    private String destName = "";

    /**
     * @param line0
     * @param line1
     * @param line2
     * @param line3
     */

    LiftSign (Config config, String line0, String line1, String line2, String line3) {
        this.config = config;
        this.sign0 = line0;
        this.sign1 = line1;
        this.sign2 = line2;
        this.sign3 = line3;
        if (line0.isEmpty())
            signVersion = Config.signVersion;
        else if (line0.split(":").length == 1)
            readVersion1();
        else if (line0.split(":").length == 2)
            readVersion2();

        System.out.print(Integer.toString(signVersion));
        if (signVersion < 2)
            updateFormat();
    }

    private void readVersion1(){
        try {
            signVersion = 1;
            this.currentFloor = Integer.parseInt(this.sign1);
            this.destFloor = Integer.parseInt(this.sign2.split(":")[1].trim().replaceAll("\\§r", ""));
            this.destName = this.sign3;
        } catch (Exception e){
            this.currentFloor = 0;
            this.destFloor = 0;
        }
    }

    private void readVersion2(){
        try {
            signVersion = 2;
            this.currentFloor = Integer.parseInt(this.sign0.split(":")[1].trim().replaceAll("\\§r", ""));
            this.currentName = this.sign1;
            this.destFloor = Integer.parseInt(this.sign2.split(":")[1].trim().replaceAll("\\§r", ""));
            this.destName = this.sign3;
        } catch (Exception e){
            this.currentFloor = 0;
            this.destFloor = 0;
        }

    }

    private void updateFormat(){
        if (signVersion == 1){
            setCurrentFloor(Integer.parseInt(this.sign1));
            signVersion = 2;
        }
    }

    void setCurrentFloor(int currentFloor) {
        this.sign0 = Config.stringCurrentFloor + ": " + Integer.toString(currentFloor) + "§r";
    }

    int getCurrentFloor() {
        try{
            String[] splits = sign0.split(": ");
            return Integer.parseInt(splits[1].replaceAll("\\s","").replaceAll("\\§r", ""));
        } catch (Exception e){
            return 0;
        }
    }

    int getDestinationFloor() {
        try{
            String[] splits = sign2.split(": ");
            return Integer.parseInt(splits[1].replaceAll("\\s","").replaceAll("\\§r", ""));
        } catch (Exception e){
            return 0;
        }
    }

    void setDestinationFloor(int destination){
        this.sign2 = this.config.stringDestination + ": " + Integer.toString(destination) + "§r";
    }

    void setDestinationName(String destinationName) {
        this.sign3 = destinationName + "§r";
    }

    String[] saveSign() {
        String[] data = new String[4];
        data[0] = this.sign0;
        data[1] = this.sign1;
        data[2] = this.sign2;
        data[3] = this.sign3;
        return data;
    }

    void setCurrentName(String name){
        sign1 = name + "§r";
    }
}
