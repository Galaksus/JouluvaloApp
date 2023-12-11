package com.example.myapplication.BluetoothLE;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.JavaScriptInterface;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BLEHandler {
    private Map<UUID, BluetoothGattCharacteristic> writeQueue = new HashMap<>();
    private final Context mContext;
    private final Activity mActivity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private boolean scanning;
    private Handler handler = new Handler();
    public static boolean isBluetoothSupported;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BT_SCAN = 1;

    private static final String TAG = "BluetoothScanActivity";
    private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
    private static final UUID SERVICE_UUID = UUID.fromString("58ecb6f1-887b-487d-a378-0f9048c505da");
    public static final UUID DELAY_CHARACTERISTIC_UUID = UUID.fromString("e0a432d7-8e2c-4380-b4b2-1568aa0412a3");
    public static final UUID DIMMER_CHARACTERISTIC_UUID = UUID.fromString("20e88205-d8cd-42a9-bcfa-4b599484d362");
    public static final UUID MANUAL_MODE_DATA_CHARACTERISTIC_UUID = UUID.fromString("2f926b0c-c378-474e-8ced-3194b815aedd");
    public static final UUID MODE_CHARACTERISTIC_UUID = UUID.fromString("5ff534ba-c2f2-4e41-8350-f016bbb2bf0f");
    public static final UUID OUTBOARDMOTOR_CHARACTERISTIC_UUID = UUID.fromString("f53de08c-1c0c-459a-a6d5-cd26a1523060");

    private LayoutInflater mInflater;

    private LeDeviceListAdapter leDeviceListAdapter = new LeDeviceListAdapter(mInflater);

    // Constructor
    public BLEHandler(@NonNull Activity activity, Context context) {
        mContext = context;
        mActivity = activity;

        // Create bluetoothManager and bluetoothAdapter
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Activity.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


        // Test if bluetooth is supported on the devive
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.e(TAG, "Bluetooth not supported on this device");
            isBluetoothSupported = false;
        } else {
            isBluetoothSupported = true;
        }
    }

    public void startLeDeviceScanning() {
        //TODO TÄSSÄ BUGI JOKA ILMAANTUU SILLON KUN KERRAN ON JO CONNECTATTU JA SEN JÄLKEEN DISCONNECTAAN
        // JA KUN SILLOIN YRITTÄÄ UUDESTANA (JOS LAITE EI SIIS PÄÄLLÄ) ELI PITÄIS TULLA DEVICE NOT FOUND MUTTA EI TULE
        // KORJAUS OLIS EHKÄ PALAUTTAA SINGLETONI POIS, MUTTA MIETI SITÄ :D

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth ei päällä");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            // Request BLUETOOTH_CONNECT permission
            if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT);
                return;
            }
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        } else {
            Log.e(TAG, "Bluetooth on päällä");

            // Request BLUETOOTH_SCAN permission
            if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BT_SCAN);
                return;
            }

            // Initializes BluetoothLeScanner
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

            // Starts scan for Bluetooth LE devices
            scanLeDevice();
        }
    }

    private void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    // Just check the permission, it is granted earlier
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    // stop scanning
                    bluetoothLeScanner.stopScan(leScanCallback);

                    // Check if the specific device was found during the scan
                    boolean specificDeviceFound = false;
                    for (int i = 0; i < leDeviceListAdapter.getCount(); i++) {
                        BluetoothDevice device = leDeviceListAdapter.getDevice(i);
                        if (device.getName() != null && device.getName().equals("ESP32 Service")) {
                            specificDeviceFound = true;
                            break;
                        }
                    }
                    // If not found print it
                    if (!specificDeviceFound) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int deviceWasNotFound = 11; // An id to tell javaScript that device was not found
                                JavaScriptInterface.callJavaScriptFunction("setBluetoothConnectionStateText('" + deviceWasNotFound + "');");
                            }
                        });
                    }
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);

            // Call JavaScript from UI thread to set connection state accordingly
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int scanningStarted = 12; // An id to tell javaScript that scanning is started
                    JavaScriptInterface.callJavaScriptFunction("setBluetoothConnectionStateText('" + scanningStarted + "');");

                }
            });


        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_ENABLE_BT);
                        return;
                    }

                    leDeviceListAdapter.addDevice(result.getDevice());
                    leDeviceListAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Found BLE device: " + result.getDevice().getName() + " (" + result.getDevice().getAddress() + ")");
                    /* Here we connect to the ESP32 device, this happens automatically to a device named "ESP32 Service"
                     * But there could be other ways for example to let user decide where to connect (needs modifications if that is desired) */
                    if (result.getDevice().getName() != null && result.getDevice().getName().equals("ESP32 Service")) {
                        connectToBleDevice(result.getDevice());
                    }
                }
            };

    private void connectToBleDevice(BluetoothDevice device) {
        // Stop the BLE device scan
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothLeScanner.stopScan(leScanCallback);
        // Create a GATT connection to the selected BLE device
        bluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            Log.e("oma tagi", "BluetoothProfile.STATE_CONNECTED: " + String.valueOf(BluetoothProfile.STATE_CONNECTED));

            // Call JavaScript from UI thread to set connection state accordingly
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JavaScriptInterface.callJavaScriptFunction("setBluetoothConnectionStateText('" + newState + "');");

                }
            });


            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e(TAG, "State == CONNECTED");
                // Connection established, start discovering services
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                // Discover BLE services
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "State == DISCONNECTED");
                // Connection lost or disconnected
                bluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

                // Services discovered, you can now read or write characteristics
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    // Process each discovered service
                    UUID serviceUUID = service.getUuid();
                    Log.d(TAG, "services UUID: " + serviceUUID.toString());
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        // Process each discovered characteristic
                        UUID characteristicUUID = characteristic.getUuid();
                        Log.d(TAG, "Characteristic UUID: " + characteristicUUID.toString());
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            /* Not in use */
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Process the received value
                byte[] value = characteristic.getValue();
                String stringValue = new String(value, StandardCharsets.UTF_8);  // Convert using UTF-8 encoding
                Log.e(TAG, "Luettu data: " + stringValue);

                // convert to int and set it to seekBar value
                int newMotorPosition = Integer.parseInt(stringValue);
                // TODO tee kutsu HTML:ään jossa asetetaan motor poistion range sliderin arvo alla olevalla tavalla
                //menuDialog.setManualModeSeekbarProgress(newMotorPosition);

            } else {
                Log.e(TAG, "Characteristic read failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Write operation succeeded
                // Log.e(TAG, "Write operation succeeded");
            } else {
                Log.e(TAG, "Write operation failed with status: " + status);
            }
            // Move on to the next characteristic in the queue
            writeNextCharacteristic();
        }
    };


    // TODO queue

    public boolean writeCharacteristicWithData(UUID characteristicUUID, String data/*byte[] data*/) {

        Log.e(TAG, "UUID: "+characteristicUUID + " Data: "+data);


        // Check that bluetoothGatt is not null
        if (bluetoothGatt == null) {
            Log.e(TAG, "bluetoothGatt is null");
            return false;
        }

        // Get the BluetoothGattCharacteristic for writing
        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        // Check that service is not null
        if (service == null) {
            Log.e(TAG, "Service not found");
            return false;
        }

        // Gets desired charasteristic into a variable
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);

        ///////////
        // Set the value of the characteristic
        if (characteristic != null) {
            characteristic.setValue(data);
        }
        // request permission check
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        boolean success = bluetoothGatt.writeCharacteristic(characteristic);
        // Check if sending data was successful
        if (success) {
            Log.e(TAG, "Data was sent successfully ");
        } else {
            Log.e(TAG, "Failed to send data");
        }
        return success;
        ///////////
        /*
        if (writeQueue.containsKey(characteristicUUID)) {
            // If the characteristic is already in the queue, replace it with the new one
            writeQueue.put(characteristicUUID, characteristic);
        } else {
            // If the characteristic is not in the queue, add it
            writeQueue.put(characteristicUUID, characteristic);

            // If the queue was empty, start the write process
            if (writeQueue.size() == 1) {

                String strData = new String(data);
                // Convert byte array to String using the default character encoding (UTF-8)
                // Set the data for the specified characteristic
                characteristic.setValue(data);

                // Permission check for BLUETOOTH
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "No permission to send BLE data");
                    return;
                }

                // Reliably write the queue of the data I want to send
                //bluetoothGatt.beginReliableWrite();

                bluetoothGatt.writeCharacteristic(characteristic); // Deprecated

                // Write the characteristic to the connected BLE device
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Api level 33 required that's why surrounded with this
                    //bluetoothGatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                }
            }
        }
         */
    }

    private void writeNextCharacteristic() {
        if (!writeQueue.isEmpty()) {
            BluetoothGattCharacteristic characteristic = writeQueue.remove(writeQueue.keySet().iterator().next());

            // Access the data of the characteristic
            byte[] data = characteristic.getValue();

            // Permission check
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No permission to send BLE data");
                return;
            }


            bluetoothGatt.writeCharacteristic(characteristic); // Deprecated call

            // Write the next characteristic to the connected BLE device with WRITE_TYPE_DEFAULT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Api level 33 required that's why surrounded with this
                //nt testi123 =
               // bluetoothGatt.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
              /* if (testi123 == 0) {
                    Log.e(TAG, "Write successful");
                }
                else {
                    Log.e(TAG, "Write failed with status code: " + testi123);
                }*/
            }
        } else {
            // Queue is empty, all characteristics have been written
        }

        Log.e(TAG, "Write queue len: "+writeQueue.size());

    }



        // TODO tänne send ja read metodit
}
