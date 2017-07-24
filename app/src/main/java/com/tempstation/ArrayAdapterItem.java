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

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.testbluetooth.R;

import java.util.Date;
import java.util.List;

public class ArrayAdapterItem extends ArrayAdapter<TempStation> {

    private final List<TempStation> list;
    private final Activity context;

    public ArrayAdapterItem(Activity context, List<TempStation> list) {
        super(context, R.layout.listsec, list);
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        Date current = new Date();
        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.listsec, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = view.findViewById(R.id.textView1);
            viewHolder.date = view.findViewById(R.id.textView2);

            view.setTag(viewHolder);

        } else {
            view = convertView;

        }
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.text.setText(list.get(position).getTemp() + " °C  "
                + list.get(position).getHygro() + " %  "
                + list.get(position).getLocation());

        if (list.get(position).getLowBat() == 1) {
            holder.text.setTextColor(Color.RED);
            holder.date.setTextColor(Color.RED);
        } else if (list.get(position).gettingOld(current)) {
            holder.text.setTextColor(Color.YELLOW);
            holder.date.setTextColor(Color.YELLOW);
        } else {
            holder.text.setTextColor(Color.BLACK);
            holder.date.setTextColor(Color.BLACK);
        }

        holder.date.setText(list.get(position).getDate() + " Min "
                + list.get(position).getTempMin() + "  Max  "
                + list.get(position).getTempMax());

        return view;
    }

    static class ViewHolder {
        protected TextView text;
        protected TextView date;
    }
}
