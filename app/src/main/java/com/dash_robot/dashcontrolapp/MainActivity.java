package com.dash_robot.dashcontrolapp;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DashControlApp";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    public static BluetoothGatt bluetoothGatt;
    public static BluetoothGattCharacteristic dashCharacteristic;

    private TextView tvStatus;

    // Handler for delayed operations like stopping scan
    private Handler handler = new Handler(Looper.getMainLooper());

    private final UUID SERVICE_UUID = UUID.fromString("af237777-879d-6186-1f49-deca0e85d9c1");
    private final UUID COMMAND_CHARACTERISTIC_UUID = UUID.fromString("af230002-879d-6186-1f49-deca0e85d9c1");

    private final byte[] LEFT_EAR_RED_COMMAND = new byte[]{
            (byte) 0x0b, (byte) 0xFF, (byte) 0x00, (byte) 0x00
    };

    private  byte[] HI_NOISE_COMMAND;

    // Runnable for stopping scan
    private final Runnable stopScanRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "stopScanRunnable executed. Attempting to stop scan.");
            stopBleScan(); // Call the encapsulated stop scan method
            if (bluetoothGatt == null || bluetoothGatt.getConnectionState(bluetoothAdapter.getRemoteDevice(bluetoothGatt.getDevice().getAddress())) != BluetoothProfile.STATE_CONNECTED) {
                updateStatus("Dash not found or not connected within timeout.");
            }
        }
    };


    //public MainActivity() {
        //byte[] rawNoiseBytes = hexStringToByteArray("53595354444153485f48495f564f0b00c900");
        //HI_NOISE_COMMAND = new byte[1 + rawNoiseBytes.length];
        //HI_NOISE_COMMAND[0] = (byte) 0x18; // The 'say' command prefix
        //System.arraycopy(rawNoiseBytes, 0, HI_NOISE_COMMAND, 1, rawNoiseBytes.length);
    //}


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        byte[] rawNoiseBytes = hexStringToByteArray("53595354444153485f48495f564f0b00c900");
        HI_NOISE_COMMAND = new byte[1 + rawNoiseBytes.length];
        HI_NOISE_COMMAND[0] = (byte) 0x18; // The 'say' command prefix
        System.arraycopy(rawNoiseBytes, 0, HI_NOISE_COMMAND, 1, rawNoiseBytes.length);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnScan = findViewById(R.id.btnScanAndConnect);
        Button btnSendRedEar = findViewById(R.id.btnSendCommand);
        Button btnSayHi = findViewById(R.id.btnSayHi);
        Button btnStartLearning = findViewById(R.id.button);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            updateStatus("Bluetooth not available on this device.");
            btnScan.setEnabled(false); // Disable buttons if no Bluetooth
            btnSendRedEar.setEnabled(false);
            btnSayHi.setEnabled(false);
            return; // Exit onCreate if no Bluetooth
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            updateStatus("BLE Scanner not available. Ensure Bluetooth is on and permissions granted.");
            btnScan.setEnabled(false);
            btnSendRedEar.setEnabled(false);
            btnSayHi.setEnabled(false);
            return;
        }

        btnScan.setOnClickListener(v -> {
            updateStatus("🔍 Scanning...");
            startBleScan(); // Encapsulate scan start logic
        });

        btnSendRedEar.setOnClickListener(v -> {
            sendCommand(LEFT_EAR_RED_COMMAND, "LEFT EAR RED");
        });

        btnSayHi.setOnClickListener(v -> {
            sendCommand(HI_NOISE_COMMAND, "HI noise");
        });
        btnStartLearning.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LearningActivity.class);
            startActivity(intent);
        });
        requestPermissions();
    }

    // --- Lifecycle Methods for Robustness ---
    @Override
    protected void onPause() {
        super.onPause();
        // Stop scan when activity is paused (going to background)
        stopBleScan();
        // Disconnect GATT if still connected (optional, but good for resource saving)
        // If you want to maintain connection in background, skip this or use foreground service
        //disconnectAndCloseGatt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-initialize Bluetooth components if they were released in onPause/onDestroy
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (bluetoothAdapter != null && bleScanner == null) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        // If you disconnected in onPause, you might want to auto-reconnect here
        // For simplicity, we'll rely on the user clicking "Scan and Connect" again
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called. Cleaning up resources.");
        // Crucial: Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacks(stopScanRunnable);

        // Ensure scanner is stopped
        stopBleScan(); // Call the encapsulated stop scan method

        // Ensure GATT connection is closed and reference is cleared
        disconnectAndCloseGatt(); // Call the encapsulated GATT cleanup method

        // Nullify other references if they could be large or cause issues
        bleScanner = null;
        bluetoothAdapter = null;
        tvStatus = null; // Clear UI reference
        Log.d(TAG, "Resources cleaned up.");
    }

    // --- Helper Methods ---

    // Helper method to consolidate sending commands
    private void sendCommand(byte[] command, String commandName) {
        if (dashCharacteristic != null && bluetoothGatt != null) {
            dashCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            dashCharacteristic.setValue(command);
            // Check for BLUETOOTH_CONNECT permission before writeCharacteristic
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                updateStatus("BLUETOOTH_CONNECT permission not granted for writing. Cannot send " + commandName + ".");
                requestPermissions();
                return;
            }
            boolean result = bluetoothGatt.writeCharacteristic(dashCharacteristic);
            updateStatus(result ? "Attempting to send " + commandName + " command..." : "Failed to initiate " + commandName + " write.");
        } else {
            updateStatus("Not connected or command characteristic missing for " + commandName + ".");
        }
    }

    private void updateStatus(String msg) {
        runOnUiThread(() -> tvStatus.setText(msg));
        Log.d(TAG, "Status: " + msg); // Log status messages for debugging
    }

    // Encapsulate BLE scan start logic
    private void startBleScan() {
        if (bluetoothAdapter == null || bleScanner == null) {
            updateStatus("Bluetooth or BLE Scanner unavailable.");
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            updateStatus("Bluetooth is off. Please enable it.");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            updateStatus("BLUETOOTH_SCAN permission not granted. Requesting...");
            requestPermissions();
            return;
        }

        // Stop any previous scan before starting a new one
        stopBleScan(); // Ensures we don't have multiple scans running

        updateStatus("🔍 Scanning...");
        try {
            bleScanner.startScan(scanCallback);
            handler.postDelayed(stopScanRunnable, 10000); // Schedule stopping the scan after 10 seconds
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during startScan: " + e.getMessage());
            updateStatus("Permission error during scan: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during startScan: " + e.getMessage());
            updateStatus("Error starting scan: " + e.getMessage());
        }
    }

    // Encapsulate BLE scan stop logic
    private void stopBleScan() {
        if (bleScanner != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                try {
                    bleScanner.stopScan(scanCallback);
                    handler.removeCallbacks(stopScanRunnable); // Make sure runnable is cancelled
                    Log.d(TAG, "BLE Scan stopped.");
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during stopScan: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error during stopScan: " + e.getMessage());
                }
            } else {
                Log.w(TAG, "Cannot stop scan: BLUETOOTH_SCAN permission not granted.");
            }
        }
    }

    // Encapsulate GATT disconnection and closure logic
    private void disconnectAndCloseGatt() {
        if (bluetoothGatt == null) {
            return;
        }
        Log.d(TAG, "Attempting to disconnect and close GATT.");
        // Attempt graceful disconnect first
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            try {
                bluetoothGatt.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting BluetoothGatt: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Cannot disconnect GATT: BLUETOOTH_CONNECT permission not granted.");
        }

        // Always close, regardless of disconnect success, to release resources
        try {
            bluetoothGatt.close();
            Log.d(TAG, "BluetoothGatt closed successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error closing BluetoothGatt: " + e.getMessage());
        } finally {
            bluetoothGatt = null; // Absolutely crucial to nullify the reference
            dashCharacteristic = null; // Also nullify characteristic
        }
    }


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();

            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted during onScanResult.");
                // We shouldn't stop scan or connect without permission, just log
                return;
            }

            String deviceName = device.getName();
            if (deviceName != null && deviceName.toLowerCase(Locale.ROOT).contains("dash")) {
                Log.d(TAG, "Dash device found: " + deviceName);
                stopBleScan(); // Stop scanning once Dash is found

                updateStatus("Found " + deviceName + ". Connecting...");

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted before connectGatt.");
                    return;
                }
                try {
                    device.connectGatt(MainActivity.this, false, gattCallback);
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during connectGatt: " + e.getMessage());
                    updateStatus("Permission error during connect: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error during connectGatt: " + e.getMessage());
                    updateStatus("Error connecting to device: " + e.getMessage());
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            updateStatus("BLE Scan Failed: " + errorCode);
            Log.e(TAG, "BLE Scan Failed with error code: " + errorCode);
            handler.removeCallbacks(stopScanRunnable); // Ensure runnable is removed on failure
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: status=" + status + ", newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt = gatt;
                updateStatus("Connected. Discovering services...");
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted before discoverServices.");
                    return;
                }
                try {
                    gatt.discoverServices();
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException during discoverServices: " + e.getMessage());
                    updateStatus("Permission error during service discovery: " + e.getMessage());
                    disconnectAndCloseGatt(); // Clean up on error
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error during discoverServices: " + e.getMessage());
                    updateStatus("Error discovering services: " + e.getMessage());
                    disconnectAndCloseGatt(); // Clean up on error
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                updateStatus("Disconnected.");
                disconnectAndCloseGatt(); // Crucial: Always clean up GATT when disconnected
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                // Handle connection failures explicitly (e.g., GATT_ERROR, GATT_CONN_TIMEOUT)
                updateStatus("Connection failed with status: " + status + ". Disconnecting and closing GATT.");
                Log.e(TAG, "GATT Connection Failure: status=" + status);
                disconnectAndCloseGatt(); // Clean up on any failure
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: status=" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    dashCharacteristic = service.getCharacteristic(COMMAND_CHARACTERISTIC_UUID);
                    bluetoothGatt = gatt;
                    if (dashCharacteristic != null) {
                        int props = dashCharacteristic.getProperties();
                        updateStatus("Command Characteristic props: " + props);
                        if ((props & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                                (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            updateStatus("Ready to send commands. Command characteristic is writable.");
                        } else {
                            updateStatus("Command characteristic not writable (lacks WRITE or WRITE_NO_RESPONSE properties). Verify COMMAND_CHARACTERISTIC_UUID.");
                            dashCharacteristic = null; // Clear reference if not writable
                        }
                    } else {
                        updateStatus("Command Characteristic not found with UUID: " + COMMAND_CHARACTERISTIC_UUID.toString() + ". Verify it's correct.");
                    }
                } else {
                    updateStatus("Service not found with UUID: " + SERVICE_UUID.toString());
                }
            } else {
                updateStatus("Service discovery failed with status: " + status);
                Log.e(TAG, "Service discovery failed with status: " + status);
                disconnectAndCloseGatt(); // Clean up on service discovery failure
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: UUID=" + characteristic.getUuid() + ", status=" + status);
            if (characteristic.getUuid().equals(COMMAND_CHARACTERISTIC_UUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    updateStatus("Command acknowledged by Dash's Bluetooth (GATT_SUCCESS).");
                } else {
                    updateStatus("Command write failed at GATT layer. Status: " + status + ". Check byte format or permissions.");
                    // Consider disconnecting/reconnecting on persistent write failures
                }
            }
        }
    };

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                updateStatus("All necessary Bluetooth permissions granted.");
                // Re-check scanner and adapter status if permissions just granted
                if (bluetoothAdapter == null) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter == null) {
                        updateStatus("Bluetooth not available on this device.");
                    }
                }
                if (bluetoothAdapter != null && bleScanner == null) {
                    bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                    if (bleScanner == null) {
                        updateStatus("BLE Scanner not available. Ensure Bluetooth is on.");
                    }
                }
            } else {
                updateStatus("Bluetooth permissions not fully granted. Functionality may be limited.");
            }
        }
    }


    /**
     * Helper method to convert a hex string to a byte array.
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    public static void writeCommand(byte[] command) {
        Log.d("BLE_DEBUG", "GATT object: " + bluetoothGatt);
        Log.d("BLE_DEBUG", "Characteristic object: " + dashCharacteristic);

        if (bluetoothGatt != null && dashCharacteristic != null) {
            dashCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            dashCharacteristic.setValue(command);
            Log.d("BLE_COMMAND", Arrays.toString(command));

            try {
                bluetoothGatt.writeCharacteristic(dashCharacteristic);
            } catch (SecurityException e) {
                Log.e(TAG, "Missing permission to writeCharacteristic: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error writing to characteristic: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Cannot write command: Not connected or characteristic unavailable.");
        }
    }

}