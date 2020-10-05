package kr.co.jjnet.bonego;

/**
 * Copyright (C) 2017 JJNET Co., Ltd
 * 모든 권리 보유.
 * Developed by JJNET Co., Ltd.
 **/

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;

import static java.lang.Float.compare;

public class jjRecognizingLoop extends Thread {
    Context paCtx;

    private final String TAG = "jjRecognizingLoop";
    private volatile boolean boRunning = true;
    private volatile boolean boPaused = false;
    private kr.co.jjnet.bonego.jjRealTimeFourieTransform stft;
    private final jjRecgMetaInfo jjRecgMetaInfo;

    private double[] spectrumDBcopy;
    private kr.co.jjnet.bonego.jjMakeSinWave sineGen1;
    private kr.co.jjnet.bonego.jjMakeSinWave sineGen2;

    private final kr.co.jjnet.bonego.MainActivity activity;

    volatile double wavSecRemain;
    volatile double wavSec = 0;

    kr.co.jjnet.bonego.jjReadSoundPattern readSnd  = kr.co.jjnet.bonego.jjReadSoundPattern.getInstance(); // Sound Pattern Singleton
    kr.co.jjnet.bonego.jjSingleton sgl  = kr.co.jjnet.bonego.jjSingleton.getInstance(); //Singleton

    double mOffsetdB = 10;  // Offset for bar, i.e. 0 lit LEDs at 10 dB.
    // The Google ASR input requirements state that audio input sensitivity
    // should be set such that 90 dB SPL at 1000 Hz yields RMS of 2500 for
    // 16-bit samples, i.e. 20 * log_10(2500 / mGain) = 90.
    double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);
    // For displaying error in calibration.
    double mDifferenceFromNominal = 0.0;
    double mRmsSmoothed;  // Temporally filtered version of RMS.
    double mAlpha = 0.9;  // Coefficient of IIR smoothing filter for RMS.

    public jjRecognizingLoop(kr.co.jjnet.bonego.MainActivity _activity, jjRecgMetaInfo _recgMetaInfo) {
        activity = _activity;
        paCtx = activity.getApplicationContext();
        jjRecgMetaInfo = _recgMetaInfo;
        boPaused = false;

        // Signal sources for testing
        double fq0 = Double.parseDouble(activity.getString(R.string.signal_1_freq1));
        double amp0 = Math.pow(10, 1/20.0 * Double.parseDouble(activity.getString(R.string.signal_1_db1)));
        double fq1 = Double.parseDouble(activity.getString(R.string.signal_2_freq1));
        double amp1 = Math.pow(10, 1/20.0 * Double.parseDouble(activity.getString(R.string.signal_2_db1)));
        double fq2 = Double.parseDouble(activity.getString(R.string.signal_2_freq2));
        double amp2 = Math.pow(10, 1/20.0 * Double.parseDouble(activity.getString(R.string.signal_2_db2)));
        if (jjRecgMetaInfo.audioSourceId == 1000) {
            sineGen1 = new kr.co.jjnet.bonego.jjMakeSinWave(fq0, jjRecgMetaInfo.sampleRate, jjRecgMetaInfo.SAMPLE_VALUE_MAX * amp0);
        } else {
            sineGen1 = new kr.co.jjnet.bonego.jjMakeSinWave(fq1, jjRecgMetaInfo.sampleRate, jjRecgMetaInfo.SAMPLE_VALUE_MAX * amp1);
        }
        sineGen2 = new kr.co.jjnet.bonego.jjMakeSinWave(fq2, jjRecgMetaInfo.sampleRate, jjRecgMetaInfo.SAMPLE_VALUE_MAX * amp2);
    }

    private void SleepWithoutInterrupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private double baseTimeMs = SystemClock.uptimeMillis();

    private void LimitFrameRate(double updateMs) {
        // Limit the frame rate by wait `delay' ms.
        baseTimeMs += updateMs;
        long delay = (int) (baseTimeMs - SystemClock.uptimeMillis());
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Log.i(TAG, "Sleep interrupted");  // seems never reached
            }
        } else {
            baseTimeMs -= delay;  // get current time
        }
    }

    private double[] mdata;

    // Generate test data.
    private int readTestData(short[] a, int offsetInShorts, int sizeInShorts, int id) {
        if (mdata == null || mdata.length != sizeInShorts) {
            mdata = new double[sizeInShorts];
        }
        Arrays.fill(mdata, 0.0);
        switch (id - 1000) {
            case 1:
                sineGen2.getSamples(mdata);
            case 0:
                sineGen1.addSamples(mdata);
                for (int i = 0; i < sizeInShorts; i++) {
                    a[offsetInShorts + i] = (short) Math.round(mdata[i]);
                }
                break;
            case 2:
                for (int i = 0; i < sizeInShorts; i++) {
                    a[i] = (short) (jjRecgMetaInfo.SAMPLE_VALUE_MAX * (2.0*Math.random() - 1));
                }
                break;
            default:
                Log.w(TAG, "readTestData(): No this source id = " + jjRecgMetaInfo.audioSourceId);
        }
        // Block this thread, so that behave as if read from real device.
        LimitFrameRate(1000.0*sizeInShorts / jjRecgMetaInfo.sampleRate);
        return sizeInShorts;
    }

    @Override
    public void run() {
        AudioRecord record;

        long tStart = SystemClock.uptimeMillis();
        long tEnd = SystemClock.uptimeMillis();
        if (tEnd - tStart < 500) {
            SleepWithoutInterrupt(500 - (tEnd - tStart));
        }

        int minBytes = AudioRecord.getMinBufferSize(jjRecgMetaInfo.sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (minBytes == AudioRecord.ERROR_BAD_VALUE) {
            return;
        }

        int readChunkSize    = jjRecgMetaInfo.hopLen;
        readChunkSize        = Math.min(readChunkSize, 2048);
        int bufferSampleSize = Math.max(minBytes / jjRecgMetaInfo.BYTE_OF_SAMPLE, jjRecgMetaInfo.fftLen/2) * 2;

        bufferSampleSize = (int)Math.ceil(1.0 * jjRecgMetaInfo.sampleRate / bufferSampleSize) * bufferSampleSize;

        try {
            record = new AudioRecord(jjRecgMetaInfo.audioSourceId,
                                    jjRecgMetaInfo.sampleRate,
                                    AudioFormat.CHANNEL_IN_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    jjRecgMetaInfo.BYTE_OF_SAMPLE * bufferSampleSize);

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Fail to initialize recorder.");
            return;
        }

        jjRecgMetaInfo.sampleRate = record.getSampleRate();

        if (record.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "jjRecognizingLoop::run(): Fail to initialize AudioRecord()");
            return;
        }

        short[] audioSamples = new short[readChunkSize];
        int numOfReadShort;

        stft = new jjRealTimeFourieTransform(activity, jjRecgMetaInfo);
        stft.setAWeighting(jjRecgMetaInfo.isAWeighting);
        if (spectrumDBcopy == null || spectrumDBcopy.length != jjRecgMetaInfo.fftLen/2+1) {
            spectrumDBcopy = new double[jjRecgMetaInfo.fftLen/2+1];
        }

        jjMonitorSound monitorSound = new jjMonitorSound(jjRecgMetaInfo.sampleRate, bufferSampleSize, "jjRecognizingLoop::run()");
        monitorSound.start();

        // Start recording
        try {
            record.startRecording();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Fail to start recording.");
            return;
        }


        // TODO: allow change of FFT length on the fly.
        while (boRunning) {

            numOfReadShort = record.read(audioSamples, 0, readChunkSize);   // pulling

            if ( monitorSound.updateState(numOfReadShort) ) {  // performed a check
                if (monitorSound.getLastCheckOverrun()) {
                }
            }

            if (boPaused) {
                continue;
            }

            stft.feedData(audioSamples, numOfReadShort);

            // If there is new spectrum data, do plot
            if (stft.nElemSpectrumAmp() >= jjRecgMetaInfo.nFFTAverage) {
                // Update spectrum or spectrogram
                final double[] spectrumDB = stft.getSpectrumAmpDB();
                System.arraycopy(spectrumDB, 0, spectrumDBcopy, 0, spectrumDB.length);

                // get RMS
                double dtRMS = stft.getRMS();
                double dtRMSFromFT = stft.getRMSFromFT();
                // rewind original double data rms
                dtRMS *= 32768.0F; // 정수형 rms 를 위해 double 값을 변환한 것을 복귀해야 RMS 가 나온다.
                // Compute a smoothed version for less flickering of the display.
                mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * dtRMS;
                double rmsdB = 20.0 * Math.log10(mGain * mRmsSmoothed);

//                int offsetdB = 40; //노트 오프셋을 줬지만 화재경보4번 틀어도 안튼것과 비슷
                int offsetdB = 36; //s4 dB 엄청 느림. 오프셋을 줬지만 큰소리는 생각보다 안올라감
//                int offsetdB = 34; // 이것은 마이크에 따라 오프셋을 줘서 튜닝하는 값임.
                                   // 현재 VoIP용 7번 마이크 소스를 쓰기 때문에 6번보다 dB이 낮게 들어온다.
                                   // 그런데 문제가 있다. 잔향제거 및 자동 Gain Control 기능 때문에 dB이 막 변한다.
                                   // 그래서 6번을 써야겠다. 6번도 소리가 한번들어가면 Gain 이 올라가서 데시벨이 높게 나오는 현상이 있음.
                rmsdB = offsetdB + rmsdB;
//                Log.i(TAG, "dBSPL: " + rmsdB);
                sgl.dbVal = (int)rmsdB;

                if(sgl.dropCheckTime() == true) {   //3초동안 dB 이 drop 몇번 되는지 체크하라
                    sgl.dropSavedTime = 0;
                    stft.decideWhatKindOfSound(rmsdB, true);
                }else {
                    stft.decideWhatKindOfSound(rmsdB, false);
                }
//                if(sgl.dropCheckTime() == true) {   //3초동안 dB 이 drop 몇번 되는지 체크하라
//                    sgl.dropSavedTime = 0;
//                    stft.decideWhatKindOfSound3(rmsdB, true);
//                }else {
//                    stft.decideWhatKindOfSound3(rmsdB, false);
//                }

//                if (rmsdB > 65) {
//                    activity.showToast("소리가 감지 되었습다", 3);
//                }

                // This is finding Peak routine..
//                stft.calculatePeak();

//                boolean fire = stft.calculatePeakFire();
//                Log.d("tag", "cal_fire : "+fire);



//                if (fire == false) {
                    //calcuPeakSound();

//                    float bell = stft.calculatePeakBell();
////                Log.d("tag", "cal_bell : "+bell);
//                    float baby = stft.calculatePeakBaby();
////                Log.d("tag", "cal_baby : "+baby);
//                    int result = compare(bell, baby);
//                    if( result == 0) {
//                        Log.d("jjRecognizingLoop", "compare Equals");
//                    } else if( result < 0 ) {
//                        final String text = "벨이 울리고 있습니다.";
//                        int idx = 0;
//                        activity.showToast(text, idx);
//                        sgl.peakSound = true;
//                        sgl.setNowTime();
//                        activity.setBTSoc(true);
//
//                        byte[] data = new byte[1];
//                        data[0] = 0x1d;
//                        sgl.sendBTNotice(data);
//                    } else {
//                        //Log.d("TAG", "아기울음 근접도 HIGH = " + dist);
//                        final String text = "아기가 울고 있습니다.";
//                        int idx = 1;
//                        activity.showToast(text, idx);
//                        sgl.peakSound = true;
//                        sgl.setNowTime();
//                        activity.setBTSoc(true);
//
//                        byte[] data = new byte[1];
//                        data[0] = 0x1a;
//                        sgl.sendBTNotice(data);
//                    }

//                }
//                int idx = -1;
//                float minVal = -1;
//                if (bell < baby) {
//                    idx = 0;
//                    minVal = bell;
//                } else {
//                    idx = 1;
//                    minVal = baby;
//                }
//
//                if (fire > minVal) {
//                    idx = 2;
//                    minVal = fire;
//                }

                //stft.calculatePeakkk();

                //if (minVal < 1000000)
                    //Log.d("tag", "cal_idx: "+idx+" / minVal: "+minVal);

//                if (sgl.caluTime() == true)
//                    activity.setBTSoc(false);
//                if (sgl.caluTime() == true && sgl.mode1 == false)
//                    activity.setBTSoc(false);

//                for (int idx = 0; idx < readSnd.getSoundPatternArr().size(); idx++) {
//                    stft.calculatePeak(idx);
//                }

            }
        }

        record.stop();
        record.release();
    }

    void setAWeighting(boolean isAWeighting) {
        if (stft != null) {
            stft.setAWeighting(isAWeighting);
        }
    }

    void setPause(boolean pause) {
        this.boPaused = pause;
    }

    boolean getPause() {
        return this.boPaused;
    }

    public void finish() {
        boRunning = false;
        interrupt();
    }

    public void calcuPeakSound() {
        calcuPeakAsyncTask asyncTask = new calcuPeakAsyncTask();
        asyncTask.execute();
    }

    public class calcuPeakAsyncTask extends AsyncTask<String,Void,String> {

        public String result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            float bell = stft.calculatePeakBell();
//                Log.d("tag", "cal_bell : "+bell);
            float baby = stft.calculatePeakBaby();
//                Log.d("tag", "cal_baby : "+baby);
            int result = compare(bell, baby);
            if( result == 0) {
                Log.d("jjRecognizingLoop", "compare Equals");
            } else if( result < 0 ) {
                final String text = "벨이 울리고 있습니다.";
                int idx = 0;
                activity.showToast(text, idx);
                sgl.peakSound = true;
                sgl.setNowTime();
//                activity.setBTSoc(true);

                byte[] data = new byte[1];
                data[0] = 0x1d;
                sgl.sendBTNotice(data);
            } else {
                //Log.d("TAG", "아기울음 근접도 HIGH = " + dist);
                final String text = "아기가 울고 있습니다.";
                int idx = 1;
                activity.showToast(text, idx);
                sgl.peakSound = true;
                sgl.setNowTime();
//                activity.setBTSoc(true);

                byte[] data = new byte[1];
                data[0] = 0x1a;
                sgl.sendBTNotice(data);
            }

            return "a";
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

        }
    }
}