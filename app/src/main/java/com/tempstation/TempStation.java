/*
    Tempstation
    
    Copyright (C) 2016 Mandl

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.tempstation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//another class to handle item's id and name
public class TempStation {

    private int Id;
    // Rest aktiv ?
    private int Reset;
    // low Batt ativ
    private int LowBat;
    // Temp data
    private String Temp;
    // Date
    private Date mDate;


    // Location Name
    private String location;
    // Hygro
    private String Hygro;
    private Double TempMin;
    private Double TempMax;

    private String timeFormat = "yyyy-MM-dd HH:mm";
    private SimpleDateFormat sdf;


    // constructor
    public TempStation(int Id, String itemName, Date currentDate, int Reset,
                       int LowBat, String location, String hygro) {
        Double num;

        sdf = new SimpleDateFormat(timeFormat, Locale.US);
        this.Id = Id;
        this.Temp = itemName;
        this.mDate = currentDate;
        this.Reset = Reset;
        this.LowBat = LowBat;
        this.Hygro = hygro;

        // set min max

        num = Double.parseDouble(itemName);

        this.TempMin = num;
        this.TempMax = num;

        if (location == null) {
            this.location = "ID: " + Id + " !";
        } else
            this.location = location;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getTempMin() {

        return Double.toString(TempMin);
    }

    public String getTempMax() {

        return Double.toString(TempMax);
    }

    public void ClearMinMax() {
        this.TempMin = (double) 99;

        this.TempMax = (double) -99;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {

        return sdf.format(mDate);

    }

    public void setDate(Date currentDate) {
        mDate = currentDate;
    }

    public String getTemp() {
        return Temp;
    }

    public void setTemp(String strTemp) {

        Double num;
        Temp = strTemp;

        // update min max
        num = Double.parseDouble(strTemp);
        if (num < TempMin) {
            TempMin = num;
        }

        if (num > TempMax) {
            TempMax = num;
        }

    }

    public boolean CheckOld(Date currentDate) {
        return currentDate.getTime() - mDate.getTime() > 5 * 60 * 1000;
    }

    public boolean gettingOld(Date currentDate) {
        return currentDate.getTime() - mDate.getTime() > 2 * 60 * 1000;
    }


    public int getReset() {
        return Reset;
    }

    public void setReset(int reset) {
        Reset = reset;
    }

    public int getLowBat() {
        return LowBat;
    }

    public void setLowBat(int lowBat) {
        LowBat = lowBat;
    }

    public String getHygro() {
        return Hygro;
    }

    public void setHygro(String hygro) {
        Hygro = hygro;
    }

}
