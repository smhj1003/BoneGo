package kr.co.jjnet.bonego;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.jjnet.bonego.TabFragment.TabFragment01;
//import kr.co.jjnet.jjsmarthelmet.TabFragment.TabFragment02;
//import kr.co.jjnet.jjsmarthelmet.TabFragment.TabFragment03;
//import kr.co.jjnet.jjsmarthelmet.TabFragment.TabFragment04;
//import kr.co.jjnet.jjsmarthelmet.model.jjHistory;

/**
 * Copyright (C) 2017 JJNET Co., Ltd
 * 모든 권리 보유.
 * Developed by JJNET Co., Ltd.
 **/

public class MainActivity extends AppCompatActivity
        implements TabFragment01.OnFragmentInteractionListener
//        , TabFragment02.OnFragmentInteractionListener , TabFragment03.OnFragmentInteractionListener, TabFragment04.OnFragmentInteractionListener, DeviceFragment.OnFragmentInteractionListener
        {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

//    public jjDBHelper mDBHelper;
    public List<String> patternArr = new ArrayList<String>();
    public List<String> exPatternArr = new ArrayList<String>();
    public List<String> bellPatternArr = new ArrayList<String>();
    public List<String> firePatternArr = new ArrayList<String>();
    public TabFragment01 selectedFragment = null;
    public int tabIdx = 0;
    public  Toast alertToast;
    jjSingleton sgl  = jjSingleton.getInstance(); //Singleton

    EditText editID;
    TextView textView;
    String curMode;
    String result;
    String result2;
    int wordResult;
    Switch sw;

    TabFragment01 tabFragment01;
    FragmentManager manager;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

//            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.navigation_tab1:
                    tabIdx = 0;
                    getSupportActionBar().setTitle(R.string.tab1);
                    FragmentTransaction transaction01 = getSupportFragmentManager().beginTransaction();
                    selectedFragment = TabFragment01.newInstance("Andy","James");
                    transaction01.replace(R.id.content, selectedFragment);
                    transaction01.commit();
                    return true;
//                case R.id.navigation_tab2:
//                    tabIdx = 1;
//                    getSupportActionBar().setTitle(R.string.tab2);
//                    FragmentTransaction transaction02 = getSupportFragmentManager().beginTransaction();
//                    selectedFragment = TabFragment02.newInstance("Andy","James");
//                    transaction02.replace(R.id.content, selectedFragment);
//                    transaction02.commit();
//                    return true;
//                case R.id.navigation_tab3:
//                    tabIdx = 2;
//                    getSupportActionBar().setTitle(R.string.tab3);
//                    FragmentTransaction transaction03 = getSupportFragmentManager().beginTransaction();
//                    selectedFragment = TabFragment03.newInstance("Andy","James");
//                    transaction03.replace(R.id.content, selectedFragment);
//                    transaction03.commit();
//                    return true;
//                case R.id.navigation_tab4:
//                    tabIdx = 3;
//                    getSupportActionBar().setTitle(R.string.tab4);
//                    FragmentTransaction transaction04 = getSupportFragmentManager().beginTransaction();
//                    selectedFragment = TabFragment04.newInstance("Andy","James");
//                    transaction04.replace(R.id.content, selectedFragment);
//                    transaction04.commit();
//                    return true;

            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setContentView(R.layout.login_activity);



        getAppKeyHash();
        buttonStart = (Button)findViewById(R.id.buttonStart);
        textResult = (TextView)findViewById(R.id.textResult);
        spinnerMode = (Spinner)findViewById(R.id.spinnerMode);
        editID = (EditText)findViewById(R.id.editID);

        textView =(TextView)findViewById(R.id.textView);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        editID.setText(settings.getString("client-id", "YOUR_CLIENT_ID"))
        ;
//        tabFragment01 = (TabFragment01) getSupportFragmentManager().findFragmentById(R.id.startImageButton);

        ArrayList<String> modeArr = new ArrayList<>();
        modeArr.add("한국어인식");
        modeArr.add("영어인식");
        modeArr.add("영어발음평가");
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, modeArr);
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
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("client-id", v.getText().toString());
                    editor.apply();
                }
                return false;
            }
        });


        buttonStart.setOnClickListener(new  View.OnClickListener() {
            public void onClick(View v) {
                if (isRecording) {
                    forceStop = true;
                } else {
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
                                threadRecog.interrupt();
                            }
                        }).start();
                    } catch (Throwable t) {
                        textResult.setText("ERROR: " + t.toString());
                        forceStop = false;
                        isRecording = false;
                    }
                }
            }
        });
        // Splash
        Intent sp = new Intent(this, jjSplashActivity.class);
        //startActivityForResult(sp, 1);
        startActivity(sp);

//        setAngelBandDB();

        getSupportActionBar().setTitle(R.string.tab1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("위치 정보 액세스 권한");
                builder.setMessage("이 앱이 표지를 감지 할 수 있도록 위치 정보 액세스 권한을 부여하십시오.");
                builder.setPositiveButton(android.R.string.ok, null);;
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            }
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content, TabFragment01.newInstance("What","Ever"));
        transaction.commit();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        BottomNavigationViewHelper.disableShiftMode(navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

//        //asset_test
//        AssetManager assetMgr = this.getAssets();
//        String assets[] = null;
//        try {
//            assets = assetMgr.list("");
//
//            for(String element : assets) {
//                String[] sub = assetMgr.list(element);
//
//                if(sub.length > 0) {
//                    // TODO: 서브파일이 존재, 디렉토리
//                }
//                else {
//                    // TODO: 서브파일 없음, 파일이거나 빈 디렉토리
//                }
//                Log.d("assets",element);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        listAssetFiles(0);
        listAssetFiles(1);
        listAssetFiles(2);
        listAssetFiles(3);
    }

    private boolean listAssetFiles(int pathIdx) {

        String path;
        switch (pathIdx) {
            case 0:
                path = "pattern";
                break;
            case 1:
                path = "expattern";
                break;
            case 2:
                path = "bellpattern";
                break;
            case 3:
                path = "firepattern";
                break;
            default:
                path = "pattern";
                break;
        }

        String [] list;
        try {
            list = getAssets().list(path);
            if (list.length > 0) {
                // This is a folder
                for (String file : list) {
//                    patternArr.add(file);
//                    if (!listAssetFiles(path + "/" + file))
//                        return false;
                    if (pathIdx == 0)
                        patternArr.add(file);
                    else if (pathIdx == 1)
                        exPatternArr.add(file);
                    else if (pathIdx == 2)
                        bellPatternArr.add(file);
                    else if (pathIdx == 3)
                        firePatternArr.add(file);

                    //Log.d("assets",file);
                }
            } else {
                // This is a file
                // TODO: add file name to an array list
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri){

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
//    public void setAngelBandDB() {
//        // Check exists database
//        mDBHelper = new jjDBHelper(this);
//
//        File database = getApplicationContext().getDatabasePath(mDBHelper.DBNAME);
//        if(false == database.exists()) {
//            mDBHelper.getReadableDatabase();
//            //Copy db
//            if(copyDatabase(this)) {
//                mDBHelper.openDatabase();
//                mDBHelper.closeDatabase();
//            }
//            else {
//                return;
//            }
//        }
//    }

//    private boolean copyDatabase(Context context) {
//        try {
//            InputStream inputStream = context.getAssets().open(mDBHelper.DBNAME);
//            String DB_PATH = "";
//            if(android.os.Build.VERSION.SDK_INT >= 4.2){
//                DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
//            }
//            else{
//                DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
//            }
//            String outFileName = DB_PATH + mDBHelper.DBNAME;
//
//            OutputStream outputStream = new FileOutputStream(outFileName);
//            byte[]buff = new byte[1024];
//            int length = 0;
//            while ((length = inputStream.read(buff)) > 0) {
//                outputStream.write(buff, 0, length);
//            }
//            outputStream.flush();
//            outputStream.close();
//            Log.w("MainActivity","DB copied");
//            return true;
//        }catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    public void showDecibel(final int idx)
//    {
//        runOnUiThread(new Runnable() {
//            public void run()
//            {
//            if (tabIdx == 0) {
//                TabFragment01 fragment = (TabFragment01) getSupportFragmentManager().getFragments().get(0);
//                if(fragment != null)
//                    fragment.showDecibel();
//            }
//            }
//        });
//    }

    public void showToast(final String toast, final int idx)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {

                if (sgl.mode2 == true) {

                    //토스트
                    if (alertToast != null)
                        alertToast.cancel();

                    //alertToast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();

                    //진동
//                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//                    vibe.vibrate(500);

                    //깜빡임
                    if (tabIdx == 0) {
                        TabFragment01 fragment = (TabFragment01) getSupportFragmentManager().getFragments().get(0);
                        fragment.blinkblinkImage(idx);
                    }
                }

                //db입력
//                jjHistory history = new jjHistory("angelband", "bandID", toast, "contents", "location", "mic", "noise", "date", "1", "2", "3");
//                mDBHelper = new jjDBHelper(MainActivity.this);
//                mDBHelper.openDatabase();
//                mDBHelper.addHistory(history);
            }
        });
    }

    public void setReadSound() {
        //감지 중지
        if (tabIdx == 0) {
            TabFragment01 fragment = (TabFragment01) getSupportFragmentManager().getFragments().get(0);
            fragment.unReadSound();
        }
    }

    public void setBTSoc(boolean onoff) {
        //btsoc on
        if (tabIdx == 0) {
            TabFragment01 fragment = (TabFragment01) getSupportFragmentManager().getFragments().get(0);
            if (onoff == true)
                fragment.setBtScoOn();
            else
                fragment.setBtScoOff();
        }
    }

//    public  void onFragmentChange(int idx) {
//        if (idx == 0) {
//            selectedFragment = TabFragment04.newInstance("Andy","James");
//            getSupportFragmentManager().beginTransaction().replace(R.id.content, selectedFragment).commit();
//        }else if (idx == 1) {
//            selectedFragment = DeviceFragment.newInstance("Andy","James");
//            getSupportFragmentManager().beginTransaction().replace(R.id.content, selectedFragment).commit();
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("blank", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }
    public static final String PREFS_NAME = "prefs";
    private static final String MSG_KEY = "status";

    Button buttonStart;
    TextView textResult;
    Spinner spinnerMode;


    int maxLenSpeech = 16000 * 45;
    byte [] speechData = new byte [maxLenSpeech * 2];
    int lenSpeech = 0;
    boolean isRecording = false;
    boolean forceStop = false;

    private final Handler handler = new Handler() {
        @Override
        public synchronized void handleMessage(Message msg) {
            Bundle bd = msg.getData();
            String v = bd.getString(MSG_KEY);
            switch (msg.what) {
                // 녹음이 시작되었음(버튼)
                case 1:
                    textResult.setText(v);
                    buttonStart.setText("PUSH TO STOP");
                    break;
                // 녹음이 정상적으로 종료되었음(버튼 또는 max time)
                case 2:
                    textResult.setText(v);
                    buttonStart.setEnabled(false);
                    break;
                // 녹음이 비정상적으로 종료되었음(마이크 권한 등)
                case 3:
                    textResult.setText(v);
                    buttonStart.setText("PUSH TO START");
                    break;
                // 인식이 비정상적으로 종료되었음(timeout 등)
                case 4:
                    textResult.setText(v);
                    buttonStart.setEnabled(true);
                    buttonStart.setText("PUSH TO START");
                    break;
                // 인식이 정상적으로 종료되었음 (thread내에서 exception포함)
                case 5:
                    textResult.setText(StringEscapeUtils.unescapeJava(result));
                    buttonStart.setEnabled(true);
                    buttonStart.setText("PUSH TO START");
                    if(wordResult==1) {
                        textView.setText("전원 ON");
                        break;
                    }else if(wordResult==2) {
                        textView.setText("전원 OFF");
                        break;
                    }else if(wordResult==3) {
                        textView.setText("좌회전신호");
                        break;
                    }else if(wordResult==4) {
                        textView.setText("우회전신호");
                        break;
                    }else if(wordResult==5) {
                        textView.setText("전등 켜짐");
                        break;
                    }else if(wordResult==6) {
                        textView.setText("전등 꺼짐");
                        break;
                    }else if(wordResult==7) {
                        textView.setText("네비 작동");
                        break;
                    }else if(wordResult==8) {
                        textView.setText("119 전화");
                        break;
                    }else if(wordResult==9) {
                        textView.setText("음악 재생");
                        break;
                    }else if(wordResult==10) {
                        textView.setText("현재 시각");
                        break;
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

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
            int bufferSize = AudioRecord.getMinBufferSize(
                    16000, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord audio = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000, // sampling frequency
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            lenSpeech = 0;
            if (audio.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("ERROR: Failed to initialize audio device. Allow app to access microphone");
            }
            else {
                short [] inBuffer = new short [bufferSize];
                forceStop = false;
                isRecording = true;
                audio.startRecording();
                while (!forceStop) {
                    int ret = audio.read(inBuffer, 0, bufferSize);
                    for (int i = 0; i < ret ; i++ ) {
                        if (lenSpeech >= maxLenSpeech) {
                            forceStop = true;
                            break;
                        }
                        speechData[lenSpeech*2] = (byte)(inBuffer[i] & 0x00FF);
                        speechData[lenSpeech*2+1] = (byte)((inBuffer[i] & 0xFF00) >> 8);
                        lenSpeech++;
                    }
                }
                audio.stop();
                audio.release();
                isRecording = false;
            }
        } catch(Throwable t) {
            throw new RuntimeException(t.toString());
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
                speechData, 0, lenSpeech*2, Base64.NO_WRAP);

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
            if ( responseCode == 200 ) {
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

    private void getAppKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md;
                md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String something = new String(Base64.encode(md.digest(), 0));
                Log.e("Hash key", something);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            Log.e("name not found", e.toString());
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
}
