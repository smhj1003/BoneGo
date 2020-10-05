//package kr.co.jjnet.jjsmarthelmet;
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothGattCharacteristic;
//import android.bluetooth.BluetoothManager;
//import android.bluetooth.le.BluetoothLeScanner;
//import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanResult;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.IBinder;
//import android.support.v4.app.Fragment;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
//import android.widget.ListView;
//import android.widget.Toast;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import kr.co.jjnet.jjsmarthelmet.TabFragment.DeviceAdapter;
//
//import static android.content.Context.BIND_AUTO_CREATE;
//
///**
// * A simple {@link Fragment} subclass.
// * Activities that contain this fragment must implement the
// * {@link DeviceFragment.OnFragmentInteractionListener} interface
// * to handle interaction events.
// * Use the {@link DeviceFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
//
//public class DeviceFragment extends Fragment {
//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;
//
//    private OnFragmentInteractionListener mListener;
//
//    static final String[] LIST_MENU = {"A", "B", "C"} ;
//    private BluetoothAdapter mBluetoothAdapter;
//    private Handler mHandler;
//
//    private static final int REQUEST_ENABLE_BT = 1;
//    // Stops scanning after 10 seconds.
//    private static final long SCAN_PERIOD = 10000;
//    private ArrayList<BluetoothDevice> mLeDevices;
//    private ListView deviceListview;
//    private DeviceAdapter deviceAdapter;
//
//    private String dName;
//    private String dAddress;
//
//    //private BluetoothLeService mBluetoothLeService;
//    private BluetoothGattCharacteristic mNotifyCharacteristic;
//    private BluetoothLeScanner mBLEScanner;
//
//    jjSingleton sgl  = jjSingleton.getInstance(); //Singleton
//
//    // Code to manage Service lifecycle.
//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder service) {
//            sgl.mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//            if (!sgl.mBluetoothLeService.initialize()) {
//                Log.e("demo", "Unable to initialize Bluetooth");
//                getActivity().finish();
//            }
//            // Automatically connects to the device upon successful start-up initialization.
//            sgl.mBluetoothLeService.connect(dAddress);
//
//            sgl.btConnected = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            sgl.mBluetoothLeService = null;
//        }
//    };
//
//    // Handles various events fired by the Service.
//    // ACTION_GATT_CONNECTED: connected to a GATT server.
//    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
//    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
//    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
//    //                        or notification operations.
//    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            final String action = intent.getAction();
//            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//            }
//        }
//    };
//
//    public DeviceFragment() {
//        // Required empty public constructor
//    }
//
//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment DeviceFragment.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static DeviceFragment newInstance(String param1, String param2) {
//        DeviceFragment fragment = new DeviceFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
////        return inflater.inflate(R.layout.fragment_blank, container, false);
//        final View view = inflater.inflate(R.layout.fragment_device, container, false);
//
//        mHandler = new Handler();
//
//        // Use this check to determine whether BLE is supported on the device.  Then you can
//        // selectively disable BLE-related features.
//        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            Toast.makeText(getActivity(), "ble_not_supported", Toast.LENGTH_SHORT).show();
//            getActivity().finish();
//        }
//
//        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
//        // BluetoothAdapter through BluetoothManager.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//
////        // Checks if Bluetooth is supported on the device.
////        if (mBluetoothAdapter == null) {
////            Toast.makeText(getActivity(), "error_bluetooth_not_supported", Toast.LENGTH_SHORT).show();
////            getActivity().finish();
////            return view;
////        }
//
//        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
//        // Checks if Bluetooth LE Scanner is available.
//        if (mBLEScanner == null) {
//            Toast.makeText(getActivity(), "Can not find BLE Scanner", Toast.LENGTH_SHORT).show();
//            getActivity().finish();
//            return view;
//        }
//
//        ///////////////////////////////////////////////////////////////////////////////////////////
//
//        // Adapter 생성
//        deviceAdapter = new DeviceAdapter() ;
//
//        // 리스트뷰 참조 및 Adapter달기
//        deviceListview = (ListView) view.findViewById(R.id.device_listview);
//        deviceListview.setAdapter(deviceAdapter);
//
//        // 위에서 생성한 listview에 클릭 이벤트 핸들러 정의.
//        deviceListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView parent, View v, int position, long id) {
//                // get item
//                BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position) ;
//
//                dName = device.getName() ;
//                dAddress = device.getAddress() ;
//
//                // TODO : use item data.
//
//                Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
//                getActivity().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//                sgl.conDevice = dName + " / " + dAddress;
//                ((MainActivity)getActivity()).onFragmentChange(0);
//            }
//        }) ;
//
//
////        Button callBtn = (Button) view.findViewById(R.id.button);
////        callBtn.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                byte[] data = new byte[1];
////                data[0] = 0x1a;
////
////                BluetoothGattCharacteristic characteristic = sgl.mBluetoothLeService.getCharacteristicService();
////
////                sgl.mBluetoothLeService.writeCharacteristic(characteristic, data);
////            }
////        });
//
//        return view;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
//        // fire an intent to display a dialog asking the user to grant permission to enable it.
//        if (!mBluetoothAdapter.isEnabled()) {
//            if (!mBluetoothAdapter.isEnabled()) {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            }
//        }
//
//        scanLeDevice(true);
//    }
//
//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }
//
//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     * <p>
//     * See the Android Training lesson <a href=
//     * "http://developer.android.com/training/basics/fragments/communicating.html"
//     * >Communicating with Other Fragments</a> for more information.
//     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }
//
//    /////////////////////////////////////////////////////////////////////////////////////////////
//    private void scanLeDevice(final boolean enable) {
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
////                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    mBLEScanner.stopScan(mScanCallback);
//                }
//            }, SCAN_PERIOD);
//
////            mBluetoothAdapter.startLeScan(mLeScanCallback);
//            mBLEScanner.startScan(mScanCallback);
//        } else {
////            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            mBLEScanner.stopScan(mScanCallback);
//        }
//    }
//
////    // Device scan callback.
////    private BluetoothAdapter.LeScanCallback mLeScanCallback =
////            new BluetoothAdapter.LeScanCallback() {
////
////                @Override
////                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
////                    getActivity().runOnUiThread(new Runnable() {
////                        @Override
////                        public void run() {
////                            deviceAdapter.addDevice(device);
////                            deviceAdapter.notifyDataSetChanged();
////                        }
////                    });
////                }
////            };
//
//    private ScanCallback mScanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            processResult(result);
//            Log.d("blank", "Callback onScanResult" + result.getDevice().getName());
//        }
//
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            for (ScanResult result : results) {
//                processResult(result);
//                Log.d("blank", "Callback onBatchScanResults" + result.getDevice().getName());
//            }
//        }
//
//        @Override
//        public void onScanFailed(int errorCode) {
//            Log.d("blank", "Callback onScanFailed");
//        }
//
//        private void processResult(final ScanResult result) {
//
//            if(getActivity() == null)
//                return;
//
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.d("blank", "Callback processResult" + result.getDevice().getName());
//                    deviceAdapter.addDevice(result.getDevice());
//                    deviceAdapter.notifyDataSetChanged();
//                }
//            });
//        }
//    };
//
//}
