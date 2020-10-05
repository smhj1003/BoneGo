package kr.co.jjnet.bonego;

import android.content.res.Resources;
import android.media.MediaRecorder;
/**
 * Copyright (C) 2017 JJNET Co., Ltd
 * 모든 권리 보유.
 * Developed by JJNET Co., Ltd.
 **/

public class jjRecgMetaInfo {

    // 적어도 같은 패턴 5번 이상 반복시만 알람 울림( 나중에 변경 가능)
    int MAX_DICISION_BELL_CNT = 4;
    int MAX_DICISION_CNT = 5;
    int MAX_FIREDICISION_CNT = 3;//10;
    int MAX_FIREDICISION_SAME_CNT = 20;//10;

    // boMode_1 과 boMode_2 가 모두 false 이면 보통의 상태을 말함.

    boolean boMode_1 = false;
    boolean boMode_2 = false;

    int boMode_1_cnt = 0;
    int boMode_2_cnt = 0;

    int boMode_baby_cnt = 0;
    int boMode_bell_cnt = 0;
    int boMode_fire_cnt = 0;
    int boMode_etcc_cnt = 0;

    public String wndFuncName;
    String[] audioSourceNames;
    int[] audioSourceIDs;
    //final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    //final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.DEFAULT;
    //public final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.VOICE_COMMUNICATION; // 이것이 HSP 로 블루투스 연결한 마이크 번호임..
    public final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.VOICE_RECOGNITION; // 이걸로 해야 Auto Gain 이 적용되지 않는다..
//    public final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.MIC;
    public int audioSourceId = RECORDER_AGC_OFF;
    public int sampleRate = 8000;
    public int fftLen = 2048;
    public int hopLen = 1024;
    public double overlapPercent = 50;  // = (1 - hopLen/fftLen) * 100%
    public int nFFTAverage = 2;
    public boolean isAWeighting = false;
    final int BYTE_OF_SAMPLE = 2;
    final double SAMPLE_VALUE_MAX = 32767.0;   // Maximum signal value
    public double spectrogramDuration = 4.0;

    double[] micGainDB = null;  // should have fftLen/2 elements

    public jjRecgMetaInfo(Resources res) {

        getAudioSourceNameFromIdPrepare(res);
    }

    private void getAudioSourceNameFromIdPrepare(Resources res) {
        audioSourceNames   = res.getStringArray(R.array.audio_source);
        String[] sasid = res.getStringArray(R.array.audio_source_id);
        audioSourceIDs = new int[audioSourceNames.length];
        for (int i = 0; i < audioSourceNames.length; i++) {
            audioSourceIDs[i] = Integer.parseInt(sasid[i]);
        }
    }

    String getAudioSourceName() {

        return getAudioSourceNameFromId(audioSourceId);
    }

    String getAudioSourceNameFromId(int id) {
        for (int i = 0; i < audioSourceNames.length; i++) {
            if (audioSourceIDs[i] == id) {
                return audioSourceNames[i];
            }
        }
        return ((Integer)(id)).toString();
    }
}
