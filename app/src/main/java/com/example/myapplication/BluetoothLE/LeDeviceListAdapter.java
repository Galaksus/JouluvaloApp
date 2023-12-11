package com.example.myapplication.BluetoothLE;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class LeDeviceListAdapter extends BaseAdapter {
    private List<BluetoothDevice> mLeDevices = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context mContext;
    public LeDeviceListAdapter(LayoutInflater inflater) {
        super();
        mContext = mContext;
        mInflater = inflater;
    }

    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(android.R.layout.simple_list_item_2, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView) view.findViewById(android.R.id.text1);
            viewHolder.deviceAddress = (TextView) view.findViewById(android.R.id.text2);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = mLeDevices.get(i);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return view;
        }
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText("Unknown device");
        viewHolder.deviceAddress.setText(device.getAddress());

        return view;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
