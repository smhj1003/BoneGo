//package kr.co.jjnet.jjsmarthelmet.TabFragment;
//
//import android.content.Context;
//import android.net.Uri;
//import android.os.Bundle;
//import android.support.v4.app.Fragment;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.CheckBox;
//import android.widget.CompoundButton;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//
//import kr.co.jjnet.jjsmarthelmet.MainActivity;
//import kr.co.jjnet.jjsmarthelmet.R;
//import kr.co.jjnet.jjsmarthelmet.jjSingleton;
//
//
///**
// * A simple {@link Fragment} subclass.
// * Activities that contain this fragment must implement the
// * {@link TabFragment04.OnFragmentInteractionListener} interface
// * to handle interaction events.
// * Use the {@link TabFragment04#newInstance} factory method to
// * create an instance of this fragment.
// */
//public class TabFragment04 extends Fragment {
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
//    private RelativeLayout deviceLayout;
//    private CheckBox mode1Check;
//    private CheckBox mode2Check;
//    private RelativeLayout findLayout;
//    private TextView tvDevice;
//
//    jjSingleton sgl  = jjSingleton.getInstance(); //Singleton
//
//    public TabFragment04() {
//        // Required empty public constructor
//    }
//
//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment TabFragment04.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static TabFragment04 newInstance(String param1, String param2) {
//        TabFragment04 fragment = new TabFragment04();
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
////        return inflater.inflate(R.layout.fragment_tab_fragment04, container, false);
//
//        final View view = inflater.inflate(R.layout.fragment_tab_fragment04, container, false);
//
//        setUI(view);
//
//        return view;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//        tvDevice.setText(sgl.conDevice);
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
//    private void setUI(View view) {
//
//        deviceLayout = (RelativeLayout) view.findViewById(R.id.deviceRelativeLayout);
//        deviceLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d("frag4", "deviceLayout");
//                ((MainActivity)getActivity()).onFragmentChange(1);
//            }
//        });
//
//        tvDevice = (TextView) view.findViewById(R.id.tvMyDevices);
//
//        mode1Check = (CheckBox) view.findViewById(R.id.mode1CheckBox);
//        mode1Check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
//                sgl.mode1 = isChecked;
//                Log.d("frag4", "mode1Check "+ sgl.mode1);
//
//                if (sgl.mode1 == false) {
//                    //sgl.readSound = false;
//                    sgl.peakSound = false;
//                }
//            }
//        });
//        mode1Check.setChecked(sgl.mode1);
//
//        mode2Check = (CheckBox) view.findViewById(R.id.mode2CheckBox);
//        mode2Check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
//                sgl.mode2 = isChecked;
//                Log.d("frag4", "mode2Check "+sgl.mode2);
//            }
//        });
//        mode2Check.setChecked(sgl.mode2);
//
//        findLayout = (RelativeLayout) view.findViewById(R.id.findRelativeLayout);
//        findLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d("frag4", "findband");
//
////                if (sgl.btConnected == true) {
////                    byte[] data = new byte[1];
////                    data[0] = 0x08;
////
////                    BluetoothGattCharacteristic characteristic = sgl.mBluetoothLeService.getCharacteristicService();
////                    sgl.mBluetoothLeService.writeCharacteristic(characteristic, data);
////
////                }
//
//                byte[] data = new byte[1];
//                data[0] = 0x08;
//                sgl.sendBTNotice(data);
//            }
//        });
//
//    }
//}
