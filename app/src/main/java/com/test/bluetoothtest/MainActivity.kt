package com.test.bluetoothtest

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.TestLooperManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.w3c.dom.Text
import java.lang.Thread.sleep
import java.nio.file.Files.size
import android.view.ViewGroup
import java.nio.file.Files.size
import android.text.method.TextKeyListener.clear
import android.view.LayoutInflater
import android.widget.BaseAdapter





class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "BLUETOOTH_TEST"
        const val REQUEST_ENABLE_BT: Int = 1
        // Stops scanning after 10 seconds.
        private val SCAN_PERIOD: Long = 10000
    }

    private lateinit var mTvDevice : TextView
    private lateinit var mBtScan : Button

    private val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mScanning: Boolean = false
    private var mHandler: Handler? = null

    private val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    private var mapPairedDevices = HashMap<String,String>()
    private var mapDiscoveredDevices = HashMap<String,String>()

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var action = intent!!.action

            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT) // Bluetooth is disconnected, do handling here
                }
            }
            when(action){
                BluetoothAdapter.ACTION_STATE_CHANGED ->
                    if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF){
                        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT) // Bluetooth is disconnected, do handling here
                    }
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    var device : BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    mapDiscoveredDevices[device.address] = device.name
                    Log.e(TAG,""+device.address+" : "+device.name)
//                    var deviceName = device.getName()
//                    var deviceHardwareAddress = device.getAddress(); // MAC address
                }
            }

//            when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)){
//                BluetoothAdapter.STATE_OFF -> startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT) // Bluetooth is disconnected, do handling here
//            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mTvDevice = findViewById(R.id.tv_device)
        mBtScan = findViewById(R.id.bt_scan)

        if(mBluetoothAdapter == null){
            Log.d(TAG,"bluetooth feature not supported by this device")
        }else{
            if(!mBluetoothAdapter.isEnabled){
                startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
            }
        }

        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mBroadcastReceiver, filter)

        fillUI()
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BT){
            when(resultCode){
                Activity.RESULT_OK -> Log.d(TAG,"Bluetooth activated")
                Activity.RESULT_CANCELED -> Log.d(TAG,"Bluetooth not activated")
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(mBroadcastReceiver)
        super.onDestroy()
    }

    private fun getPairedDevices() {
        val pairedDevices = mBluetoothAdapter.bondedDevices
        pairedDevices.forEach { device -> mapPairedDevices[device.address] = device.name }
    }

    private fun fillUI(){
        var stringBuilder = StringBuilder()
        stringBuilder.append("Paired devices:\n")
        mapPairedDevices.forEach { stringBuilder.append(it.key + " : " + it.value + "\n") }
        stringBuilder.append("\nDiscovered devices:\n")
        mapDiscoveredDevices.clear()
        mapDiscoveredDevices.forEach { stringBuilder.append(it.key + " : " + it.value + "\n") }
        mTvDevice.text = stringBuilder.toString()
    }

    fun onScanClick(view : View) {
        Log.e(TAG,"start discovery")
        mBluetoothAdapter.startDiscovery()
    }

    var mLeDeviceListAdapter : LeDeviceListAdapter

// Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
    new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private fun scanLeDevice(enable : Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler?.postDelayed({
                mScanning = false
                mBluetoothAdapter.stopLeScan(mLeScanCallback)
            }, SCAN_PERIOD)

            mScanning = true
            mBluetoothAdapter.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBluetoothAdapter.stopLeScan(mLeScanCallback)
        }
    }


    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList()
        private val mInflator: LayoutInflater = this@MainActivity.layoutInflater

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size()
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view!!.findViewById(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }
            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0)
                viewHolder.deviceName.setText(deviceName)
            else
                viewHolder.deviceName.setText(R.string.unknown_device)
            viewHolder.deviceAddress.setText(device.address)
            return view
        }
    }





}
