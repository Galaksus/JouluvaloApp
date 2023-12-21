package com.example.myapplication;


import static com.example.myapplication.MainActivity.mywebView;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.example.myapplication.BluetoothLE.BLEHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaScriptInterface  {
    private Context context;
    private Activity mainActivity;
    BLEHandler blehandler;

    private SensorManager sensorManager;

    public JavaScriptInterface(Context context, Activity activity) {
        this.context = context;
        mainActivity = activity;
        blehandler = new BLEHandler(mainActivity, this.context);

    }




    @JavascriptInterface
    public void toastMessageFromJS(String toastMessage) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
    }

    @JavascriptInterface
    public void connectBluetooth() {
        // Creates new instance everytime this method is called, also calls the startLeDeviceScanning
        //blehandler = new BLEHandler(mainActivity, this.context);
        blehandler.startLeDeviceScanning();
    }

    @JavascriptInterface
    public void JSToBLEInterface(int id, int value) {
        boolean bleWriteSuccessful = false;
        bleWriteSuccessful = blehandler.writeCharacteristicWithData(getCorrectUUID(id), String.valueOf(value) /*convertedValue*/);

        Log.e("mydebug2", "Elmentti: " +id+ " value: "+value);

    }

    @JavascriptInterface
    public void BLEReadRequest() {
        Log.e("mydebug2", "ködslhg");


        blehandler.readCharacteristic();
    }


    UUID getCorrectUUID(int id){
        UUID uuid = null;
        if (id == 1) {
            uuid = BLEHandler.COMMON_CHARACTERISTIC_UUID;
        }
        else if (id == 2){
            uuid = BLEHandler.DELAY_CHARACTERISTIC_UUID;
        }
        else if (id == 3){
            uuid = BLEHandler.DIMMER_CHARACTERISTIC_UUID;
        }
        return uuid;
    }

    public static void callJavaScriptFunction(String javascriptCode){
        // Execute JavaScript function
        mywebView.evaluateJavascript(javascriptCode, null);
    }


    private String dataStringTrimmer(String currentRouteData) {
        Log.d("listan loggaus", currentRouteData);

        // Extract the coordinates using regex
        Pattern pattern = Pattern.compile("LatLng\\((\\d+\\.\\d+), (\\d+\\.\\d+)\\)");
        Matcher matcher = pattern.matcher(currentRouteData);
        List<String> coordinates = new ArrayList<>();
        while (matcher.find()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lng = Double.parseDouble(matcher.group(2));
            coordinates.add(lat + "," + lng);
        }

        // Convert the list of coordinates to a string
        return String.join(";", coordinates);
    }


    public interface LocationCallback {
        void onLocationReceived(String latitude, String longitude);
        void onLocationFailed();
    }
}

