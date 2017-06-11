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
 * Created by croxis on 4/28/17.
 */
public class LiftSign {
    int signVersion = 0; // 0=hmod, 1=lift till 55, 2=lift>=56
    private Config config;
    private String sign0 = ": 0";
    private String sign1 = "";
    String sign2 = ": 0";
    private String sign3 = "";

    /**
     * @param line0
     * @param line1
     * @param line2
     * @param line3
     */
    LiftSign (Config c, String line0, String line1, String line2, String line3) {
        config = c;
        if (line0.isEmpty())
            signVersion = 2;
        else if (line0.split(":").length == 1)
            signVersion = 1;
        else if (line0.split(":").length == 2)
            signVersion = 2;
        this.sign0 = line0;
        this.sign1 = line1;
        this.sign2 = line2;
        this.sign3 = line3;
        System.out.print(Integer.toString(signVersion));
        if (signVersion < 2)
            updateFormat();
    }

    void updateFormat(){
        if (signVersion == 1){
            setCurrentFloor(Integer.parseInt(this.sign1));
            signVersion = 2;
        }
    }

    void setCurrentFloor(int currentFloor) {
        this.sign0 = config.stringCurrentFloor + ": " + Integer.toString(currentFloor) + "§r";
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
        this.sign2 = config.stringDestination + ": " + Integer.toString(destination) + "§r";
    }

    void setDestinationName(String destinationName) {
        this.sign3 = destinationName + "§r";
    }

    String[] getDump() {
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
