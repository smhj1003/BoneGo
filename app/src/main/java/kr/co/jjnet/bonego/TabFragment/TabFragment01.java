package kr.co.jjnet.bonego.TabFragment;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kr.co.jjnet.bonego.MainActivity;
import kr.co.jjnet.bonego.R;
//import kr.co.jjnet.bonego.jjDBHelper;
import kr.co.jjnet.bonego.jjReadSoundPattern;
import kr.co.jjnet.bonego.jjRecgMetaInfo;
import kr.co.jjnet.bonego.jjRecognizingLoop;
import kr.co.jjnet.bonego.jjSingleton;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TabFragment01.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TabFragment01#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TabFragment01 extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public TabFragment01() {
        // Required empty public constructor
    }

    jjSingleton sgl  = jjSingleton.getInstance(); //Singleton

    jjRecognizingLoop recognizingThread = null;
    jjReadSoundPattern readSnd  = jjReadSoundPattern.getInstance(); // Sound Pattern Singleton
    private int count_permission_explanation = 0;
    private int count_permission_request = 0;
    private jjRecgMetaInfo recgMetaInfo = null;
    public TextView mTextMessage;
    private boolean preparedSampling = false;
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;

//    public jjDBHelper mDBHelper;

    public ImageView imgBlink;
    public ImageButton soundReadBtn;
    public ToggleButton BTToggleBtn;

    EditText editID;
    TextView textView;
    TextView textView5;
    String curMode;
    String result;
    String result2;
    int wordResult;
    Switch sw;
    public static final String PREFS_NAME = "prefs";
    private static final String MSG_KEY = "status";
    TextView textResult;
    Spinner spinnerMode;

    //int lenSpeech = 0;
    boolean isRecording = false;
    boolean recording = false;
    boolean forceStop = false;
    Context context = getActivity();


    //byte totalByteBuffer[] = new byte[60 * 8000 * 2];
    byte [] totalByteBuffer = new byte[sgl.maxLenSpeech * 2];

    private static final int RECORDER_SAMPLERAT = 8000;
    //    private static final int RECORDER_CHANNELS = ;
    private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final byte RECORDER_BPP = 16;


    private final Handler handler = new Handler() {
        @Override
        public synchronized void handleMessage(Message msg) {
            Bundle bd = msg.getData();
            String v = bd.getString(MSG_KEY);
//            System.out.println("확인확인0"+wordResult);
            switch (msg.what) {
                // 녹음이 시작되었음(버튼)
                case 1:
                    textResult.setText(v);
//                    soundReadBtn.setText("PUSH TO STOP");
                    break;
                // 녹음이 정상적으로 종료되었음(버튼 또는 max time)
                case 2:
                    textResult.setText(v);
                    soundReadBtn.setEnabled(false);
//                    System.out.println("확인확인2"+wordResult);
//                    textView5.setText(wordResult);
                    break;
                // 녹음이 비정상적으로 종료되었음(마이크 권한 등)
                case 3:
                    textResult.setText(v);
//                    soundReadBtn.setText("PUSH TO START");
                    System.out.println("확인확인3"+wordResult);
                    textView5.setText("wordResult");
                    break;
                // 인식이 비정상적으로 종료되었음(timeout 등)
                case 4:
                    textResult.setText(v);
                    soundReadBtn.setEnabled(true);
//                    soundReadBtn.setText("PUSH TO START");
//                    System.out.println("확인확인4"+wordResult);
//                    textView5.setText(wordResult);
//                    System.out.println("여기여기"+wordResult);
//                    if(wordResult==1) {
//                        sw.setChecked(true);
//                    }else {
//                        sw.setChecked(false);
//                    }
                    break;
                // 인식이 정상적으로 종료되었음 (thread내에서 exception포함)
                case 5:
                    textResult.setText(StringEscapeUtils.unescapeJava(result));
                    soundReadBtn.setEnabled(true);
//                    soundReadBtn.setText("PUSH TO START");
//                    System.out.println("확인확인5"+wordResult);
                    if(wordResult==1) {
                        textView5.setText("전원 ON");
                        break;
                    }else if(wordResult==2) {
                        textView5.setText("전원 OFF");
                        break;
                    }else if(wordResult==3) {
                        textView5.setText("좌회전신호");
                        break;
                    }else if(wordResult==4) {
                        textView5.setText("우회전신호");
                        break;
                    }else if(wordResult==5) {
                        textView5.setText("전등 켜짐");
                        break;
                    }else if(wordResult==6) {
                        textView5.setText("전등 꺼짐");
                        break;
                    }else if(wordResult==7) {
                        textView5.setText("네비 작동");
                        break;
                    }else if(wordResult==8) {
                        textView5.setText("119 전화");
                        break;
                    }else if(wordResult==9) {
                        textView5.setText("음악 재생");
                        break;
                    }else if(wordResult==10) {
                        textView5.setText("현재 시각");
                        break;
                    }
                    break;
            }
            super.handleMessage(msg);
//            sendToExtractor(result);`
        }

    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TabFragment01.
     */
    // TODO: Rename and change types and number of parameters
    public static TabFragment01 newInstance(String param1, String param2) {
        TabFragment01 fragment = new TabFragment01();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        int bufferSizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERAT,
//                RECORDER_CHANNELS,
//                RECORDER_AUDIO_ENCODING
//        );
//        // Initialize Audio Recorder.
//        AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                RECORDER_SAMPLERAT,
//                RECORDER_CHANNELS,
//                RECORDER_AUDIO_ENCODING,
//                bufferSizeInBytes
//        );
//        // Start Recording.
////        audioRecorder.startRecording();
////        checkRecordPermission();
//        audioRecorder.startRecording();
//        int numberOfReadBytes = 0;
//        byte audioBuffer[] = new byte[bufferSizeInBytes];
//        boolean recording = false;
//        float tempFloatBuffer[] = new float[3];
//        int tempIndex = 0;
//        int totalReadBytes = 0;
//        byte totalByteBuffer[] = new byte[60 * 44100 * 2];
//
//
//        // While data come from microphone.
//        while (true) {
//            float totalAbsValue = 0.0f;
//            short sample = 0;
//
//            numberOfReadBytes = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes);
//
//            // Analyze Sound.
//            for (int i = 0; i < bufferSizeInBytes; i += 2) {
//                sample = (short) ((audioBuffer[i]) | audioBuffer[i + 1] << 8);
//                totalAbsValue += (float) Math.abs(sample) / ((float) numberOfReadBytes / (float) 2);
//            }
//
//            // Analyze temp buffer.
//            tempFloatBuffer[tempIndex % 3] = totalAbsValue;
//            float temp = 0.0f;
//            for (int i = 0; i < 3; ++i)
//                temp += tempFloatBuffer[i];
//
//            if ((temp >= 0 && temp <= 350) && recording == false) {
//                Log.i("TAG", "1");
//                tempIndex++;
//                continue;
//            }
//
//            if (temp > 350 && recording == false) {
//                Log.i("TAG", "2");
//                recording = true;
//            }
//
//            if ((temp >= 0 && temp <= 350) && recording == true) {
//                Log.i("TAG", "Save audio to file.");
//
//                // Save audio to file.
//                String filepath = Environment.getExternalStorageDirectory().getPath();
//                File file = new File(filepath, "AudioRecorder");
//                if (!file.exists())
//                    file.mkdirs();
//
//                String fn = file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".wav";
//
//                long totalAudioLen = 0;
//                long totalDataLen = totalAudioLen + 36;
//                long longSampleRate = RECORDER_SAMPLERAT;
//                int channels = 1;
//                long byteRate = RECORDER_BPP * RECORDER_SAMPLERAT * channels / 8;
//                totalAudioLen = totalReadBytes;
//                totalDataLen = totalAudioLen + 36;
//                byte finalBuffer[] = new byte[totalReadBytes + 44];
//
//                finalBuffer[0] = 'R';  // RIFF/WAVE header
//                finalBuffer[1] = 'I';
//                finalBuffer[2] = 'F';
//                finalBuffer[3] = 'F';
//                finalBuffer[4] = (byte) (totalDataLen & 0xff);
//                finalBuffer[5] = (byte) ((totalDataLen >> 8) & 0xff);
//                finalBuffer[6] = (byte) ((totalDataLen >> 16) & 0xff);
//                finalBuffer[7] = (byte) ((totalDataLen >> 24) & 0xff);
//                finalBuffer[8] = 'W';
//                finalBuffer[9] = 'A';
//                finalBuffer[10] = 'V';
//                finalBuffer[11] = 'E';
//                finalBuffer[12] = 'f';  // 'fmt ' chunk
//                finalBuffer[13] = 'm';
//                finalBuffer[14] = 't';
//                finalBuffer[15] = ' ';
//                finalBuffer[16] = 16;  // 4 bytes: size of 'fmt ' chunk
//                finalBuffer[17] = 0;
//                finalBuffer[18] = 0;
//                finalBuffer[19] = 0;
//                finalBuffer[20] = 1;  // format = 1
//                finalBuffer[21] = 0;
//                finalBuffer[22] = (byte) channels;
//                finalBuffer[23] = 0;
//                finalBuffer[24] = (byte) (longSampleRate & 0xff);
//                finalBuffer[25] = (byte) ((longSampleRate >> 8) & 0xff);
//                finalBuffer[26] = (byte) ((longSampleRate >> 16) & 0xff);
//                finalBuffer[27] = (byte) ((longSampleRate >> 24) & 0xff);
//                finalBuffer[28] = (byte) (byteRate & 0xff);
//                finalBuffer[29] = (byte) ((byteRate >> 8) & 0xff);
//                finalBuffer[30] = (byte) ((byteRate >> 16) & 0xff);
//                finalBuffer[31] = (byte) ((byteRate >> 24) & 0xff);
//                finalBuffer[32] = (byte) (2 * 16 / 8);  // block align
//                finalBuffer[33] = 0;
//                finalBuffer[34] = RECORDER_BPP;  // bits per sample
//                finalBuffer[35] = 0;
//                finalBuffer[36] = 'd';
//                finalBuffer[37] = 'a';
//                finalBuffer[38] = 't';
//                finalBuffer[39] = 'a';
//                finalBuffer[40] = (byte) (totalAudioLen & 0xff);
//                finalBuffer[41] = (byte) ((totalAudioLen >> 8) & 0xff);
//                finalBuffer[42] = (byte) ((totalAudioLen >> 16) & 0xff);
//                finalBuffer[43] = (byte) ((totalAudioLen >> 24) & 0xff);
//
//                for (int i = 0; i < totalReadBytes; ++i)
//                    finalBuffer[44 + i] = totalByteBuffer[i];
//
//                FileOutputStream out;
//                try {
//                    out = new FileOutputStream(fn);
//                    try {
//                        out.write(finalBuffer);
//                        out.close();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//
//                } catch (FileNotFoundException e1) {
//                    // TODO Auto-generated catch block
//                    e1.printStackTrace();
//                }
//
//                //*/
//                tempIndex++;
//                break;
//            }
//
//            // -> Recording sound here.
//            Log.i("TAG", "Recording Sound.");
//            for (int i = 0; i < numberOfReadBytes; i++)
//                totalByteBuffer[totalReadBytes + i] = audioBuffer[i];
//            totalReadBytes += numberOfReadBytes;
//            //*/
//
//            tempIndex++;
//
//        }


        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        Log.i("TAG", " max runtime mem = " + maxMemory + "k");

//        mDBHelper = new jjDBHelper(getActivity());

        Resources res = getResources();
        recgMetaInfo = new jjRecgMetaInfo(res);
        recgMetaInfo.sampleRate   = 8000;
        recgMetaInfo.fftLen       = 1024;
        recgMetaInfo.nFFTAverage  = 1;
        recgMetaInfo.isAWeighting = false;
        readSnd.audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        readjjSoundFingerPrint();

        Log.i("asset", String.valueOf(((MainActivity)getActivity()).patternArr.size()));

    }

    @Override
    public void onResume() {
        super.onResume();

        if (sgl.readSound == true) {
            LoadAngelPref();
            preparedSampling = true;

            startMonitoringSound(recgMetaInfo);

            soundReadBtn.setImageResource(R.drawable.antenna_on);
            imgBlink.setImageResource(R.drawable.circle_yellow);

            mTextMessage.setText("on");

        } else {
            if (recognizingThread != null) {

                recognizingThread.finish();
                try {

                    recognizingThread.join();

                } catch (InterruptedException e) {
                }

                recognizingThread = null;
            }

            soundReadBtn.setImageResource(R.drawable.antenna_default);
            imgBlink.setImageResource(R.drawable.circle_black);
            mTextMessage.setText("off");
        }

        setResumeToggle();
        Log.d("tab1", "sgl.bluetoothSco = "+sgl.bluetoothSco);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recognizingThread != null) {

            recognizingThread.finish();
            try {

                recognizingThread.join();

            } catch (InterruptedException e) {
            }

            recognizingThread = null;

            sgl.peakSound = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_tab_fragment01, container, false);

        final View view = inflater.inflate(R.layout.fragment_tab_fragment01, container, false);

        mTextMessage = (TextView) view.findViewById(R.id.message);
        mTextMessage.setText("on");

        imgBlink = (ImageView) view.findViewById(R.id.blinkImageView);
        textResult = (TextView)view.findViewById(R.id.textResult);
        spinnerMode = (Spinner)view.findViewById(R.id.spinnerMode);
        editID = (EditText)view.findViewById(R.id.editID);

        textView5 =(TextView)view.findViewById(R.id.textView5);


//        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
//        editID.setText(settings.getString("client-id", "YOUR_CLIENT_ID"));
        editID.setText("9a73bfe1-e57e-4e30-833e-dc469281215c");
        ArrayList<String> modeArr = new ArrayList<>();
        modeArr.add("한국어인식");
        modeArr.add("영어인식");
        modeArr.add("영어발음평가");
        final ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_spinner_item,modeArr);
//                (
//                this, android.R.layout.simple_spinner_item, modeArr);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                curMode = parent.getItemAtPosition(pos).toString();
            }
            public void onNothingSelected(AdapterView<?> parent) {
                curMode = "";
            }
        });

        editID.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("client-id", v.getText().toString());
                    editor.apply();
                }
                return false;
            }
        });


        soundReadBtn = (ImageButton) view.findViewById(R.id.startImageButton);

//        soundReadBtn.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
////                 do something
////                if(sgl.dbVal < 63) {
//
//                if(recognizingThread == null) {
////                    new Timer().schedule(new TimerTask() {
//                    forceStop = false;
//                    recognizingThread = null;
//                    sgl.readSound = true;
//                    soundReadBtn.setImageResource(R.drawable.antenna_on);
//                    imgBlink.setImageResource(R.drawable.circle_yellow);
//
//                    LoadAngelPref();
//                    preparedSampling = true;
//                    startMonitoringSound(recgMetaInfo);
//
////                    mTextMessage.setText("on");
//
////                    forceStop = false;
//                }
//
//                      else {
////                    sgl.readSound = false;
////                    soundReadBtn.setImageResource(R.drawable.antenna_default);
////                    imgBlink.setImageResource(R.drawable.circle_black);
//
////                    mTextMessage.setText("off");
////                    if ( sgl.dbVal < 59) {
////
//                        recognizingThread.finish();
//                        try {
////
//                            recognizingThread.join();
////
//                        } catch (InterruptedException e) {
//                        }
////
//                        recognizingThread = null;
////                    }
//                    sgl.readSound = true;
//                    forceStop = true;
//                }
//                jjAlert alert = new jjAlert("1", "1", "1", "1", "1", "1", "1");
//                mDBHelper.openDatabase();
//                mDBHelper.addAlert(alert);
//                mDBHelper.getListAlert().get(0).getAlertContents();

                //blinkblinkImage();
//            }


//         if (isRecording) {
//            forceStop = true;
//            } else {
//                try {
//                    new Thread(new Runnable() {
//                        public void run() {
//                            SendMessage("Recording...", 1);
//                            try {
//                                recordSpeech();
//                                SendMessage("Recognizing...", 2);
//                            } catch (RuntimeException e) {
//                                SendMessage(e.getMessage(), 3);
//                                return;
//                            }
//
//                            Thread threadRecog = new Thread(new Runnable() {
//                                public void run() {
//                                    result = sendDataAndGetResult();
//                                    result2=result;
//                                    try {
//                                        wordCheck(result);
//                                    } catch (JSONException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                            });
//                        threadRecog.start();
//                        try {
//                            threadRecog.join(20000);
//                            if (threadRecog.isAlive()) {
//                                threadRecog.interrupt();
//                                SendMessage("No response from server for 20 secs", 4);
//                            } else {
//                                SendMessage("OK", 5);
//                            }
//                        } catch (InterruptedException e) {
//                            SendMessage("Interrupted", 4);
//                        }
//                    }
//                }).start();
//            } catch (Throwable t) {
//                textResult.setText("ERROR: " + t.toString());
//                forceStop = false;
//                isRecording = false;
//             }
//            }
//        }
//    });


        // get your ToggleButton
        BTToggleBtn = (ToggleButton) view.findViewById(R.id.toggleButton);
        // attach an OnClickListener
        BTToggleBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // your click actions go here
                if(BTToggleBtn.isChecked()){
                    //Button is ON
                    // Do Something
                    readSnd.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    readSnd.audioManager.startBluetoothSco();
                    readSnd.audioManager.setBluetoothScoOn(true);
                    //Toast.makeText(view.getContext(), "ON", Toast.LENGTH_SHORT).show();
                    sgl.bluetoothSco = true;
                    //현재 시간 저장
                    sgl.setNowTime();
                }
                else {
                    //Button is OFF
                    // Do Something
                    readSnd.audioManager.setMode(AudioManager.MODE_NORMAL);
                    readSnd.audioManager.stopBluetoothSco();
                    readSnd.audioManager.setBluetoothScoOn(false);
                    //Toast.makeText(view.getContext(), "OFF", Toast.LENGTH_SHORT).show();
                    sgl.bluetoothSco = false;
                }

//                btSco();
            }
        });

        return view;
    }



    public void SendMessage(String str, int id) {
        Message msg = handler.obtainMessage();
        Bundle bd = new Bundle();
        bd.putString(MSG_KEY, str);
        msg.what = id;
        msg.setData(bd);
        handler.sendMessage(msg);
    }

    public static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

    public void recordSpeech() throws RuntimeException {

        try {
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                    sgl.sampleRate, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord audioRecorder = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sgl.sampleRate, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes);


            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("ERROR: Failed to initialize audio device. Allow app to access microphone");
            } else {
                short[] inBuffer = new short[bufferSizeInBytes];  // 오디오 디바이스의 최소 버퍼만큼 확보
                forceStop = false;
                isRecording = true;
                audioRecorder.startRecording();
                int numberOfReadBytes = 0;
                byte audioBuffer[] = new byte[bufferSizeInBytes];
                float tempFloatBuffer[] = new float[3];
                int tempIndex = 0;
                int totalReadBytes = 0;

                sgl.setSpeechData(); // 초기화

                while (!forceStop) {
                    float totalAbsValue = 0.0f;
                    short sample = 0;

                    numberOfReadBytes = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes); // 오디오 디바이스에서 실제 읽은 바이트수가 리턴됨(640 리턴 또는 0)
//                    Log.i("speechRead", "Read -------------" + numberOfReadBytes);
                    if(numberOfReadBytes == 0)
                        continue;
                    for (int i = 0; i < bufferSizeInBytes; i += 2) {
                        sample = (short) ((audioBuffer[i]) | audioBuffer[i + 1] << 8);
                        totalAbsValue += (float) Math.abs(sample) / ((float) numberOfReadBytes / (float) 2);
//                        totalAbsValue += Math.abs( sample ) / (numberOfReadBytes/2);
                    }
//                    System.out.println("totalAbsValue"+totalAbsValue);
                    tempFloatBuffer[tempIndex % 3] = totalAbsValue;
                    float temp = 0.0f;
                    for (int i = 0; i < 3; ++i)
                        temp += tempFloatBuffer[i];
//                    Log.i("speechRead", "temp -------------" + temp + "recording ----------" + recording);
                    if ((temp >= 0 && temp <= 450) && recording == false) {
                        tempIndex++;
                        continue;
                    }
//                    Log.i("speechRead", "temp --- 1 -------------" + temp);
                    if (temp > 450 && recording == false) { //시작시점
                        Log.i("speechIn", "1. --------------------------------------------------------------------- 발화 시작");
                        recording = true;
                    }
//                    Log.i("speechRead", "temp --- 인식 중.." + temp);
                    if ((temp >= 0 && temp <= 450) && recording == true) { //종료조건
                        Log.i("speechOut", "2. --------------------------------------------------------------------- 발화 종료");
                        sgl.totalReadBytes = totalReadBytes; //  이거 해야 정확한 길이 전송 가능
                        totalReadBytes = 0; // 이걸해야 AI Hub 에 전송후 sgl.speechData[0] 부터 다시 audioBuffer 에서 읽어들임
                        recording = false;
                        //sgl.speechData ai허브에 전달
//                        Runnable interruptedThread = new InterruptedThread();
                        Thread threadRecog = new Thread(new Runnable() {
                            public void run() {
                                result = sendDataAndGetResult();
                                result2=result;
                                try {
                                    wordCheck(result);
                                    Log.i("speechOut", "5. --------------------------------------------------------------------- 명령종료");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        threadRecog.start();
                        try {
                            threadRecog.join(20000);
                            if (threadRecog.isAlive()) {
                                Thread.sleep(1);
                                SendMessage("No response from server for 20 secs", 4);
                            } else {
//                                Log.d("interrupt()전","444444444");
                                Thread.sleep(1);
                                threadRecog.interrupt();
//                                Log.d("interrupt()후","444444444");
                                SendMessage("OK", 5);
                            }
                        } catch (InterruptedException e) {
                            Thread.sleep(1);
//                            Log.d("interrupt()전","3333333333");
                            threadRecog.interrupt();
//                            Log.d("interrupt()후","333333333");
                            SendMessage("Interrupted", 4);

                        }
//                        Log.d("interrupt()전","000000000000");
                        threadRecog.interrupt();
//                        Log.d("interrupt()후","111111111111");
                        continue;

                    }
//                    Log.d("interrupt()후다음","누적시작");
                    for (int i = 0; i < numberOfReadBytes; i++) //실제 읽은 바이트 수만큼 speechData 에 기록(speechData 라는 버퍼가 훨씬 큼)
                        sgl.speechData[totalReadBytes + i] = audioBuffer[i];  // totalReadBytes 는 이전 audio.read 에서 읽은 바이트 수이므로 그 다음 바이트부터 추가로 기록한다는 뜻임

                    totalReadBytes += numberOfReadBytes;  // 총 읽은 바이트수는 매 audioBuffer 으로부터  읽은 바이트를 누적하여, speechOut 될때 aihub에 보내고 그다응 리셋해야 함.
//                    Log.d("interrupt()후다음","누적종료");
                    tempIndex++;

//                    if (sgl.dbVal < 64) {  // 이거 있으면 안됨
//                        recording = false;
//
//                    }
                }
                recording = false;
                isRecording = false;
                forceStop = true;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t.toString());
        }
    }

    public void recordSpeechOrg() throws RuntimeException {

        try {
            int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                    16000, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord audioRecorder = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes);


            if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("ERROR: Failed to initialize audio device. Allow app to access microphone");
            }
            else {
                short [] inBuffer = new short [bufferSizeInBytes];
                forceStop = false;
                isRecording = true;
                audioRecorder.startRecording();
                int numberOfReadBytes = 0;
                byte audioBuffer[] = new byte[bufferSizeInBytes];
//                boolean recording = false;
                float tempFloatBuffer[] = new float[3];
                int tempIndex = 0;
                int totalReadBytes = 0;

                sgl.setSpeechData(); // 초기화

                while (!forceStop) {

                    float totalAbsValue = 0.0f;

//                    int ret = audioRecorder.read(inBuffer, 0, bufferSizeInBytes);
                    short sample = 0;

                    numberOfReadBytes = audioRecorder.read(audioBuffer, 0, bufferSizeInBytes);
//                    for (int i = 0; i < ret ; i++ ) {
//                        if (lenSpeech >= maxLenSpeech) {
//                            forceStop = true;
//                            break;
//                        }
//                        speechData[lenSpeech*2] = (byte)(inBuffer[i] & 0x00FF);
//                        speechData[lenSpeech*2+1] = (byte)((inBuffer[i] & 0xFF00) >> 8);
//                        lenSpeech++;
//                    }
                    for (int i = 0; i < bufferSizeInBytes; i += 2) {
                        sample = (short) ((audioBuffer[i]) | audioBuffer[i + 1] << 8);
                        totalAbsValue += (float) Math.abs(sample) / ((float) numberOfReadBytes / (float) 2);
//                        totalAbsValue += Math.abs( sample ) / (numberOfReadBytes/2);
                    }
                    System.out.println("totalAbsValue"+totalAbsValue);
                    tempFloatBuffer[tempIndex % 3] = totalAbsValue;
                    float temp = 0.0f;
                    for (int i = 0; i < 3; ++i)
                        temp += tempFloatBuffer[i];
//                    System.out.println("temp"+temp);
                    if ((temp >= 0 && temp <= 350) && recording == false) {
                        System.out.println("temp0 "+temp);
//                        Log.i("TAG", "1");
                        tempIndex++;
                        continue;
                    }

                    if (temp > 350 && recording == false) { //시작시점
                        System.out.println("temp1 "+temp);
//                        Log.i("speakin","temp");
//                        Log.i("TAG", "2");
                        recording = true;
                    }

                    if ((temp >= 0 && temp <= 350) && recording == true) { //종료조건
                        //speechData ai허브에 전달
//                        System.out.println("temp3 "+temp);
//                        System.out.println(" totalByteBuffer[totalReadBytes]1 "+totalByteBuffer[totalReadBytes]);
//                        speechData = totalByteBuffer;
//                        Log.i("speakOut","temp");
//                        System.out.println(" speechData[0] "+speechData[0]);
//                        System.out.println(" totalByteBuffer[totalReadBytes]2 "+totalByteBuffer[totalReadBytes]);

                        Thread threadRecog = new Thread(new Runnable() {
                            public void run() {
                                result = sendDataAndGetResult();
                                result2=result;
                                try {
                                    wordCheck(result);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        threadRecog.start();
                        try {
                            threadRecog.join(20000);
                            if (threadRecog.isAlive()) {
                                threadRecog.interrupt();
                                SendMessage("No response from server for 20 secs", 4);
                            } else {
                                SendMessage("OK", 5);
                            }
                        } catch (InterruptedException e) {
                            SendMessage("Interrupted", 4);
                        }

                        continue;
//                        forceStop = true;
                    }

                    for (int i = 0; i < numberOfReadBytes; i++)
                        sgl.speechData[totalReadBytes + i] = audioBuffer[i];
                        System.out.println(" speechData[totalReadBytes + i]000 "+sgl.speechData[totalReadBytes]);
                        totalReadBytes += numberOfReadBytes;
                        //*/
                        tempIndex++;

                    if(sgl.dbVal < 64) {
                        recording = false;

                    }
                }
                recording=false;
                isRecording = false;
                forceStop = true;
            }
        } catch(Throwable t) {
            throw new RuntimeException(t.toString());
        }

        if(sgl.dbVal > 63) {
            forceStop = false;
            recognizingThread = null;
            sgl.readSound = true;
            soundReadBtn.setImageResource(R.drawable.antenna_on);
            imgBlink.setImageResource(R.drawable.circle_yellow);

            LoadAngelPref();
            preparedSampling = true;
            startMonitoringSound(recgMetaInfo);

        }
        else if(sgl.dbVal < 63) {
            recognizingThread.finish();
            try {
                recognizingThread.join();
            } catch (InterruptedException e) {
            }

            recognizingThread = null;
            sgl.readSound = true;
            forceStop = true;
        }
    }

    public String sendDataAndGetResult () {
        String openApiURL = "http://aiopen.etri.re.kr:8000/WiseASR/Recognition";
        String accessKey = editID.getText().toString().trim();
        String languageCode;
        String audioContents;

        Gson gson = new Gson();

        switch (curMode) {
            case "한국어인식":
                languageCode = "korean";
                break;
            case "영어인식":
                languageCode = "english";
                break;
            case "영어발음평가":
                languageCode = "english";
                openApiURL = "http://aiopen.etri.re.kr:8000/WiseASR/Pronunciation";
                break;
            default:
                return "ERROR: invalid mode";
        }

        Map<String, Object> request = new HashMap<>();
        Map<String, String> argument = new HashMap<>();

        audioContents = Base64.encodeToString(
               sgl.speechData, 0, sgl.totalReadBytes, Base64.NO_WRAP);

        Log.i("speechSnd","3. --------------------------------------------------------------------- AI 음성 분석 요청");
        argument.put("language_code", languageCode);
        argument.put("audio", audioContents);

        request.put("access_key", accessKey);
        request.put("argument", argument);

        URL url;
        Integer responseCode;
        String responBody;
        try {
            url = new URL(openApiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(gson.toJson(request).getBytes("UTF-8"));
            wr.flush();
            wr.close();

            responseCode = con.getResponseCode();
            sgl.setSpeechData();  // 초기화
            if ( responseCode == 200 ) {
                Log.i("speechSnd","4. --------------------------------------------------------------------- AI 음성 분석 완료");
                InputStream is = new BufferedInputStream(con.getInputStream());
                responBody = readStream(is);
                return responBody;
            }
            else
                return "ERROR: " + Integer.toString(responseCode);
        } catch (Throwable t) {
            return "ERROR: " + t.toString();
        }
    }

    public void wordCheck(String result) throws JSONException {

        ArrayList powerOnCommand = new ArrayList();
        powerOnCommand.add("시동");
        powerOnCommand.add("전원");
        powerOnCommand.add("파워");

        ArrayList powerOffCommand = new ArrayList();
        powerOffCommand.add("전원꺼");
        powerOffCommand.add("꺼");
        powerOffCommand.add("오프");

        ArrayList leftCommand = new ArrayList();
        leftCommand.add("좌회전");
        leftCommand.add("자회전");
        leftCommand.add("좌측");
        leftCommand.add("자측");
        leftCommand.add("왼쪽으로");
        leftCommand.add("왼쪽");

        ArrayList rightCommand = new ArrayList();
        rightCommand.add("우회전");
        rightCommand.add("무회전");
        rightCommand.add("5회전");
        rightCommand.add("우측");
        rightCommand.add("오른쪽쪽으로");
        rightCommand.add("오른쪽");

        ArrayList lightOnCommand = new ArrayList();
        lightOnCommand.add("조명");
        lightOnCommand.add("전등");
        lightOnCommand.add("불켜");
        lightOnCommand.add("불");
        lightOnCommand.add("조명켜");
        lightOnCommand.add("전등켜");

        ArrayList lightOffCommand = new ArrayList();
//       lightOffCommand.add("조명");
//       lightOffCommand.add("전등");
        lightOffCommand.add("불꺼");
//       lightOffCommand.add("불");
        lightOffCommand.add("조명꺼");
        lightOffCommand.add("전등꺼");

        ArrayList naviOnCommand = new ArrayList();
        naviOnCommand.add("네비");
        naviOnCommand.add("네비켜");
        naviOnCommand.add("길안내");
        naviOnCommand.add("내비");
        naviOnCommand.add("네비게이션");
        naviOnCommand.add("지도");

        ArrayList call119Command = new ArrayList();
        call119Command.add("119");
        call119Command.add("19");
        call119Command.add("일일구");
        call119Command.add("응급차");
        call119Command.add("응급");
        call119Command.add("일일9");
        call119Command.add("1일구");

        ArrayList musicOnCommand = new ArrayList();
        musicOnCommand.add("음악");
        musicOnCommand.add("음악재생");
        musicOnCommand.add("뮤직");
        musicOnCommand.add("music");
        musicOnCommand.add("노래");

        ArrayList askTimeCommand = new ArrayList();
        askTimeCommand.add("시간");
        askTimeCommand.add("몇시");
        askTimeCommand.add("몇시야");
        askTimeCommand.add("몇 시야");
        askTimeCommand.add("시야");
        askTimeCommand.add("지금몇시");
        askTimeCommand.add("지금시간");


        System.out.println(result);
        JSONObject jObject = new JSONObject(result);
        JSONObject return_object = jObject.getJSONObject("return_object");
        String recognized = return_object.getString("recognized");
//       System.out.println("이거"+return_object);
//       System.out.println("이거"+recognized);
        String[] resultArray = recognized.split(" ");
//       System.out.println(A);
        ArrayList<String> resultList = new ArrayList<>();

        for (int i = 0; i < resultArray.length; i++) {
            resultList.add(resultArray[i]);
        }
//       System.out.println("위치");
        System.out.println(resultList);
        System.out.println("resultArray" + resultArray);


        for (Object b : resultList) {
            if (powerOnCommand.contains(b)) {
                wordResult = 1; //전원켜기
                break;
            } else if (powerOffCommand.contains(b)) {
                wordResult = 2; //전원끄기
                break;
            } else if (leftCommand.contains(b)) {
                wordResult = 3; //좌측깜빡이 5초후 꺼지게
                break;
            } else if (rightCommand.contains(b)) {
                wordResult = 4; //우측깜빡이 5초후 꺼지게
                break;
            } else if (lightOnCommand.contains(b)) {
                wordResult = 5; //전등 켜기
                break;
            } else if (lightOffCommand.contains(b)) {
                wordResult = 6; //전등 끄기
                break;
            } else if (naviOnCommand.contains(b)) {
                wordResult = 7; //네비 켜기
                break;
            } else if (call119Command.contains(b)) {
                wordResult = 8; //119전화
                break;
            } else if (musicOnCommand.contains(b)) {
                wordResult = 9; //음악재생
                break;
            } else if (askTimeCommand.contains(b)) {
                wordResult = 10; //시간묻기
                break;
            }


        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void startMonitoringSound(final jjRecgMetaInfo _recgMetaInfo) {
        // Stop previous sampler if any.
        if (recognizingThread != null) {

            recognizingThread.finish();
            try {

                recognizingThread.join();

            } catch (InterruptedException e) {
            }

            recognizingThread = null;
        }

        if (!requestRecordPermission())
            return;

        if (!preparedSampling) {
            return;
        }

        // Start recognize sound pattern
        recognizingThread = new jjRecognizingLoop((MainActivity) getActivity(), _recgMetaInfo);
        recognizingThread.start();

//        if (isRecording) {
//            forceStop = true;
//            } else {
                try {
                    new Thread(new Runnable() {
                        public void run() {
                            SendMessage("Recording...", 1);
                            try {
                                recordSpeech();
                                SendMessage("Recognizing...", 2);
                            } catch (RuntimeException e) {
                                SendMessage(e.getMessage(), 3);
                                return;
                            }

                            Thread threadRecog = new Thread(new Runnable() {
                                public void run() {
                                    result = sendDataAndGetResult();
                                    result2=result;
                                    try {
                                        wordCheck(result);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        threadRecog.start();
                        try {
                            threadRecog.join(20000);
                            if (threadRecog.isAlive()) {
                                threadRecog.interrupt();
                                SendMessage("No response from server for 20 secs", 4);
                            } else {
                                SendMessage("OK", 5);
                            }
                        } catch (InterruptedException e) {
                            SendMessage("Interrupted", 4);
                        }
                    }
                }).start();
            } catch (Throwable t) {
                textResult.setText("ERROR: " + t.toString());
                forceStop = false;
                isRecording = false;
             }
            }

//    }

    public void showToast(final String toast)
    {
        getActivity().runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(getView().getContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void readjjSoundFingerPrint()
    {
        BufferedReader reader = null;

        try {
////            InputStream ps = getAssets().open("FFT_BabyCry_Pattern01_1Sec.txt");
////            InputStream ps = getAssets().open("FFT_BabyCry_Pattern02_1Sec.txt");
//            InputStream ps = getActivity().getApplicationContext().getAssets().open("FFT_BabyCry_Pattern03_1Sec.txt");
//            reader = new BufferedReader(new InputStreamReader(ps));
//
//            String mLine;
//            int i = 0;
//            while ((mLine = reader.readLine()) != null) {
//                readSnd.addSoundPattern(mLine, i++);
//            }

            //assets_pattern////////////////////////////////////////////////////////////////
            for (int idx = 0; idx < ((MainActivity)getActivity()).patternArr.size(); idx++) {
                Log.d("asset", ((MainActivity)getActivity()).patternArr.get(idx));
                InputStream is = getActivity().getApplicationContext().getAssets().open("pattern/"+((MainActivity)getActivity()).patternArr.get(idx));
                reader = new BufferedReader(new InputStreamReader(is));

                readSnd.initialSoundPattern();
                String lineStr;
                int lineIdx = 0;
                while ((lineStr = reader.readLine()) != null) {
                    Log.d("lineIdx", lineIdx+1 + " lineStr: " +lineStr);
                    readSnd.addSoundPattern(lineStr, lineIdx++);
                }
                readSnd.addSoundPatternArr();
            }

            //assets_expattern////////////////////////////////////////////////////////////////
            for (int idx = 0; idx < ((MainActivity)getActivity()).exPatternArr.size(); idx++) {
                Log.d("asset", ((MainActivity)getActivity()).exPatternArr.get(idx));
                InputStream is = getActivity().getApplicationContext().getAssets().open("expattern/"+((MainActivity)getActivity()).exPatternArr.get(idx));
                reader = new BufferedReader(new InputStreamReader(is));

                readSnd.initialExSoundPattern();
                String lineStr;
                int lineIdx = 0;
                while ((lineStr = reader.readLine()) != null) {
                    Log.d("lineIdx", lineIdx+1 + " lineStr: " +lineStr);
                    readSnd.addExSoundPattern(lineStr, lineIdx++);
                }
                readSnd.addExSoundPatternArr();
            }

            //assets_bellpattern////////////////////////////////////////////////////////////////
            for (int idx = 0; idx < ((MainActivity)getActivity()).bellPatternArr.size(); idx++) {
                Log.d("asset", ((MainActivity)getActivity()).bellPatternArr.get(idx));
                InputStream is = getActivity().getApplicationContext().getAssets().open("bellpattern/"+((MainActivity)getActivity()).bellPatternArr.get(idx));
                reader = new BufferedReader(new InputStreamReader(is));

                readSnd.initialBellSoundPattern();
                String lineStr;
                int lineIdx = 0;
                while ((lineStr = reader.readLine()) != null) {
                    Log.d("lineIdx", lineIdx+1 + " lineStr: " +lineStr);
                    readSnd.addBellSoundPattern(lineStr, lineIdx++);
                }
                readSnd.addBellSoundPatternArr();
            }

            //assets_firepattern////////////////////////////////////////////////////////////////
            for (int idx = 0; idx < ((MainActivity)getActivity()).firePatternArr.size(); idx++) {
                Log.d("asset", ((MainActivity)getActivity()).firePatternArr.get(idx));
                InputStream is = getActivity().getApplicationContext().getAssets().open("firepattern/"+((MainActivity)getActivity()).firePatternArr.get(idx));
                reader = new BufferedReader(new InputStreamReader(is));

                readSnd.initialFireSoundPattern();
                String lineStr;
                int lineIdx = 0;
                while ((lineStr = reader.readLine()) != null) {
                    Log.d("lineIdx", lineIdx+1 + " lineStr: " +lineStr);
                    readSnd.addFireSoundPattern(lineStr, lineIdx++);
                }
                readSnd.addFireSoundPatternArr();
            }

        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {

                }
            }
        }
    }

    private void LoadAngelPref() {

        // Load preferences for recorder and views, beside loadPreferenceForView()
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getView().getContext());

        boolean keepScreenOn = sharedPref.getBoolean("keepScreenOn", true);
        if (keepScreenOn) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        recgMetaInfo.audioSourceId = Integer.parseInt(sharedPref.getString("audioSource", Integer.toString(recgMetaInfo.RECORDER_AGC_OFF)));
        //recgMetaInfo.audioSourceId = 7;
        recgMetaInfo.wndFuncName = sharedPref.getString("windowFunction", "Hanning");
        recgMetaInfo.spectrogramDuration = Double.parseDouble(sharedPref.getString("spectrogramDuration",
                Double.toString(6.0)));
        recgMetaInfo.overlapPercent = Double.parseDouble(sharedPref.getString("fft_overlap_percent", "50.0"));
        recgMetaInfo.hopLen = (int)(recgMetaInfo.fftLen * (1 - recgMetaInfo.overlapPercent/100) + 0.5);

    }

    // API 23 부터는 명시적인 사용자 허가를 받지 않고는 마이크 입력을 받을 수 없다.
    // Manifest 만으로는 안된다..
    private boolean requestRecordPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("TAG", "Permission RECORD_AUDIO denied. Trying  to request...");
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.RECORD_AUDIO) &&
                    count_permission_explanation < 1) {
                Log.w("TAG", "  Show explanation here....");
                count_permission_explanation++;
            } else {
                Log.w("TAG", "  Requesting...");
                if (count_permission_request < 3) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                    count_permission_explanation = 0;
                    count_permission_request++;
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Context context = getActivity().getApplicationContext();
                            String text = "Permission denied.";
                            Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }
            }
            return false;
        }
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("TAG", "Permission WRITE_EXTERNAL_STORAGE denied. Trying  to request...");

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w("TAG", "RECORD_AUDIO Permission granted by user.");
                } else {
                    Log.w("TAG", "RECORD_AUDIO Permission denied by user.");
                }
                break;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Log.w("TAG", "WRITE_EXTERNAL_STORAGE Permission denied by user.");
                }
                break;
            }
        }
        // Then onResume() will be called.
    }

    public void showDecibel() {
        if(sgl.dbVal > 0) {
            mTextMessage.setText(Integer.toString(sgl.dbVal) + "dB");
        }

    }
    public void blinkblinkImage(int idx) {

        if(idx <= 3) {
            Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
            animation.setDuration(250); // duration - half a second
            animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
            animation.setRepeatCount(4); // Repeat animation infinitely
            animation.setRepeatMode(Animation.REVERSE); // Reverse animation at

            imgBlink.startAnimation(animation);
        }

        switch (idx) {
            case 0:
                soundReadBtn.setImageResource(R.drawable.bell_on);
                break;
            case 1:
                soundReadBtn.setImageResource(R.drawable.baby_on);
                break;
            case 2:
                soundReadBtn.setImageResource(R.drawable.fire_on);
                break;
            case 3:
                soundReadBtn.setImageResource(R.drawable.alert_on);
                break;
            default:
                soundReadBtn.setImageResource(R.drawable.antenna_on);
        }
    }

    public void unReadSound() {
        readSoundAsyncTask asyncTask = new readSoundAsyncTask();
        asyncTask.execute();
    }

    public class readSoundAsyncTask extends AsyncTask<String,Void,String> {

        public String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
//            ((MainActivity)getActivity()).runOnUiThread(new Runnable() {
//                public void run() {
//                    mTextMessage.setText(Integer.toString(sgl.dbVal));
//                }
//            });
            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            sgl.readSound = false;
            soundReadBtn.setImageResource(R.drawable.antenna_default);
            imgBlink.setImageResource(R.drawable.circle_black);
            mTextMessage.setText("off");

            if (recognizingThread != null) {

                recognizingThread.finish();
                try {

                    recognizingThread.join();

                } catch (InterruptedException e) {
                }

                recognizingThread = null;
            }
        }
    }


    public void setBtScoOn() {
        btScoOnAsyncTask asyncTask = new btScoOnAsyncTask();
        asyncTask.execute();
    }

    public class btScoOnAsyncTask extends AsyncTask<String,Void,String> {

        public String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if(readSnd.audioManager.isBluetoothScoOn() ==  false){
                //Button is ON
                BTToggleBtn.setChecked(true);
                readSnd.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                readSnd.audioManager.startBluetoothSco();
                readSnd.audioManager.setBluetoothScoOn(true);
                sgl.bluetoothSco = true;
            }
        }
    }

    public void setBtScoOff() {
        setBtScoOffAsyncTask asyncTask = new setBtScoOffAsyncTask();
        asyncTask.execute();
    }

    public class setBtScoOffAsyncTask extends AsyncTask<String,Void,String> {

        public String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            return result;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if(readSnd.audioManager.isBluetoothScoOn() ==  true){
                //Button is OFF
                BTToggleBtn.setChecked(false);
                readSnd.audioManager.setMode(AudioManager.MODE_NORMAL);
                readSnd.audioManager.stopBluetoothSco();
                readSnd.audioManager.setBluetoothScoOn(false);
                sgl.bluetoothSco = false;
            }
        }
    }

    public void setResumeToggle() {
        if(sgl.bluetoothSco == true){
            //Button is ON
            BTToggleBtn.setChecked(true);
//            readSnd.audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
//            readSnd.audioManager.startBluetoothSco();
//            readSnd.audioManager.setBluetoothScoOn(true);
            //현재 시간 저장
            sgl.setNowTime();
        }
        else {
            //Button is OFF
            BTToggleBtn.setChecked(false);
//            readSnd.audioManager.setMode(AudioManager.MODE_NORMAL);
//            readSnd.audioManager.stopBluetoothSco();
//            readSnd.audioManager.setBluetoothScoOn(false);
        }
    }

}
