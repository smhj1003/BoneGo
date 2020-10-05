package kr.co.jjnet.bonego;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by maro on 2017-08-23.
 */

public class jjSingleton {

    public int  dbVal = 0;
    public boolean mode1 = true;
    public boolean mode2 = true;
    public boolean readSound = true;
    public boolean peakSound = false;
    public boolean bluetoothSco = false;
    public boolean startMonitor = false;

    public boolean async = false;

    public boolean btConnected = false;
    public kr.co.jjnet.bonego.BluetoothLeService mBluetoothLeService = null;

    public int timerValue = 0;
    public long savedTime = 0;

    public String conDevice = "device";

    public long dropSavedTime = 0;

    public int bellDistCnt = 0;
    public float bellAvgDist = 0;
    public float bellTotalDist = 0;
    public int babyDistCnt = 0;
    public float babyAvgDist = 0;
    public float babyTotalDist = 0;
    public double oldRmsdB = 0;

    public int sampleRate = 14400;              // 전화 표준 샘플 Hz (8000 Bit 가 아니다 ! - 그냥 표본의 갯수가 초당 8000개란 의미이다.)
    public int maxLenSpeech = sampleRate * 45; // 최대샘플 길이로 8000 Hz x 45초 분량의 음성 샘플 길이를 설정하겠다는 뜻이다.
    public int totalReadBytes = 0;              // 실제 한 명령 구간 안에서 읽은 바이트 수(이걸 싱글톤으로 공유해야, AI허브 쓰레드에 정확한 명령구간 길이만큼만 전달할 수 있다. 아니면 찌끄러기가 남는다.
    public byte [] speechData = null;

    private jjSingleton () {
        dbVal = 0;
        mode1 = true;
        mode2 = true;
        readSound = true;
        peakSound = false;
        bluetoothSco = false;

        timerValue = 0;
        savedTime = 0;

        mBluetoothLeService = null;

        conDevice = "device";

        dropSavedTime = 0;

        bellDistCnt = 0;
        bellAvgDist = 0;
        bellTotalDist = 0;
        babyDistCnt = 0;
        babyAvgDist = 0;
        babyTotalDist = 0;

        oldRmsdB = 0;

        setSpeechData();
    }

    private static class Singleton {
        private static final jjSingleton instance = new jjSingleton();
    }

    public static jjSingleton getInstance () {
        System.out.println("create instance");
        return Singleton.instance;
    }

    public void setSpeechData() {
        speechData = null;
        speechData = new byte [maxLenSpeech]; // 결국 45초 x 2 = 90초 분량의 Byte 버퍼를 확보하겠다는 것이다.
        totalReadBytes = 0;          // 이거해야 찌그러기 안간다..
    }
    public void  setNowTime() {
        long curTime = System.currentTimeMillis();
        savedTime = curTime / (1000);
    }

    public boolean dropCheckTime() {
        boolean retVal = false;
        long curTime = System.currentTimeMillis();
        long nowTime = curTime / (1000);

        if (dropSavedTime  == 0) {
            dropSavedTime = nowTime;
            return false;
        }

        if (nowTime-dropSavedTime > 3) //3초 동안 dB 이 떨어지는지 확인용
            retVal = true;

        return retVal;
    }

    public boolean caluTime() {
        boolean retVal = false;
        long curTime = System.currentTimeMillis();
        long nowTime = curTime / (1000);

        if (savedTime  == 0) {
            savedTime = nowTime;
            return false;
        }

        if (nowTime-savedTime > 300) //300초 5분
            retVal = true;

        return retVal;
    }

    public void sendBTNotice(byte[] data) {
        if (btConnected == true && mode2 == true) {
            BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristicService();
            mBluetoothLeService.writeCharacteristic(characteristic, data);
        }
    }

}
