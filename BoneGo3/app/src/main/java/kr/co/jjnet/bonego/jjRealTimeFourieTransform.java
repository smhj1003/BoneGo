package kr.co.jjnet.bonego;

/**
 * Copyright (C) 2017 JJNET Co., Ltd
 * 모든 권리 보유.
 * Developed by JJNET Co., Ltd.
 **/

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

import com.google.corp.productivity.specialprojects.android.fft.RealDoubleFFT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static java.lang.Float.compare;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.log10;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

public class jjRealTimeFourieTransform {

    kr.co.jjnet.bonego.MainActivity mainActivity;
    Context paCtx;
    kr.co.jjnet.bonego.jjRecgMetaInfo myRecg;

    // data for frequency Analysis

//    private int APPROXIMATE = 2000000;
    private float APPROXIMATE = 800000;
    private float BELLAPPROXIMATE = 300000;
    private float FIREAPPROXIMATE = 2000000;
    private float DROP_DECIBEL = 47.0F;//40.0F;     // 암소음
    private float MID_DECIBEL = 52.0F;//60.0F;      // 보통 말소리
    private float HIGH_DECIBEL = 60.0F;//80.0F;     // 화재경보 사이렌

    private double[] spectrumAmpOutCum;
    private double[] spectrumAmpOutTmp;
    private double[] spectrumAmpOut;
    private double[] spectrumAmpOutDB;
    private double[] spectrumAmpIn;
    private double[] spectrumAmpInTmp;
    private double[] wnd;
    private double wndAmpF = 1;           // used to keep energy invariant under different window
    private int sampleRate;
    private int fftLen;
    private int hopLen;                           // control overlap of FFTs = (1 - lopLen/fftLen)*100%
    private int spectrumAmpPt;
    //    private double[][] spectrumAmpOutArray;
//    private int spectrumAmpOutArrayPt = 0;        // Pointer for spectrumAmpOutArray
    private int nAnalysed = 0;
    private RealDoubleFFT spectrumAmpFFT;
    private boolean boolAWeighting = false;
    private double cumRMS = 0;
    private int    cntRMS = 0;
    private double outRMS = 0;

    private double[] dBAFactor;    // multiply to power spectrum to get A-weighting
    private double[] micGain;

    private  int similarCnt = 0;   // 인접수치간 유사한 갯수
    private int upCnt = 0;         // 사이렌에서 높은 진동수로 올라간 횟수
    private int midCnt = 0;         // 기타소리로  모통 진동수로 올라간 횟수(말소리 음악소리 등)
    private int dropCnt = 0;       // 사이렌에서 낮은 진동수로 갑자기 떨어지는 횟수(소리가 없어지는 것도 마찬가지..)

    private double sqr(double x) { return x*x; }

    kr.co.jjnet.bonego.jjReadSoundPattern readSnd  = kr.co.jjnet.bonego.jjReadSoundPattern.getInstance(); // Sound Pattern Singleton
    Queue<Double> Frequeue = new LinkedList<Double>();
    Queue<Double> fireFrequeue34 = new LinkedList<Double>();
    Queue<Double> fireFrequeue32 = new LinkedList<Double>();
    Queue<Double> fireFrequeue26 = new LinkedList<Double>();
    Queue<Double> fireFrequeue14 = new LinkedList<Double>();
    private double firePatternFreq[] = { 3400.0F, 3200.0F, 2600.0F, 1400.0F  };
    private double inclination = 0.0F;
    private float minDist = Float.MAX_VALUE;

//    public jjDBHelper mDBHelper;

    kr.co.jjnet.bonego.jjSingleton sgl  = kr.co.jjnet.bonego.jjSingleton.getInstance(); //Singleton
    //public boolean peakSound = false;

    // Generate multiplier for A-weighting
    private void initDBAFactor(int fftlen, double sampleRate) {
        dBAFactor = new double[fftlen/2+1];
        for (int i = 0; i < fftlen/2+1; i++) {
            double f = (double)i/fftlen * sampleRate;
            double r = sqr(12200)*sqr(sqr(f)) / ((f*f+sqr(20.6)) * sqrt((f*f+sqr(107.7)) * (f*f+sqr(737.9))) * (f*f+sqr(12200)));
            dBAFactor[i] = r*r*1.58489319246111;  // 1.58489319246111 = 10^(1/5)
        }
    }

    //
    // Hanning Window 로 구현
    // 참조 : https://en.wikipedia.org/wiki/Hann_function
    // The advantage of the Hann window is very low aliasing, and the tradeoff is slightly decreased resolution (widening of the main lobe).
    //
    private void initFourieAmpByHanningWin(int fftlen) {
        wnd = new double[fftlen];
        for (int i = 0; i < wnd.length; i++) {
            wnd[i] = 0.5 * (1 - cos(2*PI*i/(wnd.length-1.))) * 2;
        }

        double normalizeFactor = 0;
        for (int i=0; i<wnd.length; i++) {
            normalizeFactor += wnd[i];
        }
        normalizeFactor = wnd.length / normalizeFactor;
        wndAmpF = 0;
        for (int i=0; i<wnd.length; i++) {
            wnd[i] *= normalizeFactor;
            wndAmpF += wnd[i]*wnd[i];
        }
        wndAmpF = wnd.length / wndAmpF;
    }

    void setAWeighting(boolean e_isAWeighting) {
        boolAWeighting = e_isAWeighting;
    }

    boolean getAWeighting() {
        return boolAWeighting;
    }

    private void init(int fftlen, int _hopLen, int sampleRate, int minFeedSize, String wndName) {
        if (minFeedSize <= 0) {
            return;
        }
        if (((-fftlen)&fftlen) != fftlen) {
            return;
        }
        this.sampleRate = sampleRate;
        fftLen = fftlen;
        hopLen = _hopLen;                          // 50% overlap by default
        spectrumAmpOutCum= new double[fftlen/2+1];
        spectrumAmpOutTmp= new double[fftlen/2+1];
        spectrumAmpOut   = new double[fftlen/2+1];
        spectrumAmpOutDB = new double[fftlen/2+1];
        spectrumAmpIn    = new double[fftlen];
        spectrumAmpInTmp = new double[fftlen];
        spectrumAmpFFT   = new RealDoubleFFT(spectrumAmpIn.length);
//        spectrumAmpOutArray = new double[(int)ceil((double)minFeedSize / (fftlen/2))][]; // /2 since half overlap
//        for (int i = 0; i < spectrumAmpOutArray.length; i++) {
//            spectrumAmpOutArray[i] = new double[fftlen/2+1];
//        }

        initFourieAmpByHanningWin(fftlen);
        initDBAFactor(fftlen, sampleRate);
        clear();
        boolAWeighting = false;
    }

    jjRealTimeFourieTransform(kr.co.jjnet.bonego.MainActivity activity, kr.co.jjnet.bonego.jjRecgMetaInfo _recgMetaInfo) {
        mainActivity = activity;
        paCtx = activity.getApplicationContext();
        myRecg = _recgMetaInfo;

        init(_recgMetaInfo.fftLen, _recgMetaInfo.hopLen, _recgMetaInfo.sampleRate, _recgMetaInfo.nFFTAverage, _recgMetaInfo.wndFuncName);
        micGain = _recgMetaInfo.micGainDB;
        if (micGain != null) {
            for (int i = 0; i < micGain.length; i++) {
                micGain[i] = pow(10, micGain[i] / 10.0);
            }
        } else {
        }
    }

    public void feedData(short[] ds) {
        feedData(ds, ds.length);
    }

    void feedData(short[] ds, int dsLen) {
        if (dsLen > ds.length) {
            Log.e("STFT", "dsLen > ds.length !");
            dsLen = ds.length;
        }
        int inLen = spectrumAmpIn.length;
        int outLen = spectrumAmpOut.length;
        int dsPt = 0;           // input data point to be read
        while (dsPt < dsLen) {
            while (spectrumAmpPt < 0 && dsPt < dsLen) {  // skip data when hopLen > fftLen
                double s = ds[dsPt++] / 32768.0;
                spectrumAmpPt++;
                cumRMS += s*s;
                cntRMS++;
            }
            while (spectrumAmpPt < inLen && dsPt < dsLen) {
                double s = ds[dsPt++] / 32768.0;
                spectrumAmpIn[spectrumAmpPt++] = s;
                cumRMS += s*s;
                cntRMS++;
            }
            if (spectrumAmpPt == inLen) {    // enough data for one FFT
                for (int i = 0; i < inLen; i++) {
                    spectrumAmpInTmp[i] = spectrumAmpIn[i] * wnd[i];
                }
                spectrumAmpFFT.ft(spectrumAmpInTmp);
                fftToAmp(spectrumAmpOutTmp, spectrumAmpInTmp);
//                System.arraycopy(spectrumAmpOutTmp, 0, spectrumAmpOutArray[spectrumAmpOutArrayPt], 0,
//                                 spectrumAmpOutTmp.length);
//                spectrumAmpOutArrayPt = (spectrumAmpOutArrayPt+1) % spectrumAmpOutArray.length;
                for (int i = 0; i < outLen; i++) {
                    spectrumAmpOutCum[i] += spectrumAmpOutTmp[i];
                }
//              For Test Log..
//                saveSpectrumAmpDB();



                nAnalysed++;
                if (hopLen < fftLen) {
                    System.arraycopy(spectrumAmpIn, hopLen, spectrumAmpIn, 0, fftLen - hopLen);
                }
                spectrumAmpPt = fftLen - hopLen;  // can be positive and negative
            }
        }
    }

    // Convert complex amplitudes to absolute amplitudes.
    private void fftToAmp(double[] dataOut, double[] data) {
        // data.length should be a even number
        double scaler = 2.0*2.0 / (data.length * data.length);  // *2 since there are positive and negative frequency part
        dataOut[0] = data[0]*data[0] * scaler / 4.0;
        int j = 1;
        for (int i = 1; i < data.length - 1; i += 2, j++) {
            dataOut[j] = (data[i]*data[i] + data[i+1]*data[i+1]) * scaler;
        }
        dataOut[j] = data[data.length-1]*data[data.length-1] * scaler / 4.0;
    }

    final double[] getSpectrumAmp() {
        if (nAnalysed != 0) {    // no new result
            int outLen = spectrumAmpOut.length;
            double[] sAOC = spectrumAmpOutCum;
            if(nAnalysed > 1) {
                Log.d("TAG", "nAnalysed =" + nAnalysed);
            }
            for (int j = 0; j < outLen; j++) {
                sAOC[j] /= nAnalysed;
            }
            if (micGain != null && micGain.length+1 == sAOC.length) {
                // no correction to phase
                // no correction to DC
                for (int j = 1; j < outLen; j++) {
                    sAOC[j] /= micGain[j-1];
                }
            }
            if (boolAWeighting) {
                for (int j = 0; j < outLen; j++) {
                    sAOC[j] *= dBAFactor[j];
                }
            }
            System.arraycopy(sAOC, 0, spectrumAmpOut, 0, outLen);
            Arrays.fill(sAOC, 0.0);
            nAnalysed = 0;
            for (int i = 0; i < outLen; i++) {
                spectrumAmpOutDB[i] = 10.0 * log10(spectrumAmpOut[i]);
            }
        }
        return spectrumAmpOut;
    }

    final double[] getSpectrumAmpDB() {
        getSpectrumAmp();
        return spectrumAmpOutDB;
    }

    double getRMS() {

        outRMS = sqrt(cumRMS / cntRMS * 2.0F);  // 노말라이즈 불필요..
        cumRMS = 0;
        cntRMS = 0;

        return outRMS;
    }

    double getRMS_2() {
        if (cntRMS > 8000/30) {
            outRMS = sqrt(cumRMS / cntRMS * 2.0);  // "* 2.0" normalize to sine wave.
            cumRMS = 0;
            cntRMS = 0;
        }
        return outRMS;
    }

    double getRMSFromFT() {
        getSpectrumAmpDB();
        double s = 0;
        for (int i = 1; i < spectrumAmpOut.length; i++) {
            s += spectrumAmpOut[i];
        }
        return sqrt(s * wndAmpF);
    }

    int nElemSpectrumAmp() {
        return nAnalysed;
    }

    double maxAmpFreq = Double.NaN, maxAmpDB = Double.NaN;
    double curFloorAmpFreq = Double.NaN;

//    void calculatePeak() {
//        getSpectrumAmpDB();
//        // Find and show peak amplitude
//        maxAmpDB  = 20 * log10(0.125/32768);
//        maxAmpFreq = 0;
//        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
//            if (spectrumAmpOutDB[i] > maxAmpDB) {
//                maxAmpDB  = spectrumAmpOutDB[i];
//                maxAmpFreq = i;
//            }
//        }
//
//        // Slightly better peak finder
//        // The peak around spectrumDB should look like quadratic curve after good window function
//        // a*x^2 + b*x + c = y
//        // a - b + c = x1
//        //         c = x2
//        // a + b + c = x3
//        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
//            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
//            double x1 = spectrumAmpOutDB[id-1];
//            double x2 = spectrumAmpOutDB[id];
//            double x3 = spectrumAmpOutDB[id+1];
//            double c = x2;
//            double a = (x3+x1)/2 - x2;
//            double b = (x3-x1)/2;
//            if (a < 0) {
//                double xPeak = -b/(2*a);
//                if (abs(xPeak) < 1) {
//                    maxAmpFreq += xPeak * sampleRate / fftLen;
//                    maxAmpDB = (4*a*c - b*b)/(4*a);
//                }
//            }
//        }
//
//        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//        Log.d("TAG", "아기울음 진동수 = " +  maxAmpFreq);
//        Frequeue.offer(maxAmpFreq);
//
//        if(Frequeue.size() >= readSnd.getPatternMaxCnt()) {  // 최초에만 비어 있고 그 다음에는 계속 55 개 이다..그러므로 항상 실시간으로 검사한다.
////            double[] sp = readSnd.getSoundPattern();
//            ArrayList<Double> sp = readSnd.getSoundPattern();
//// -----------Method 1 -------------------------------
//// 단순한 이방법은 너무 적중도가 낮다..
////            int i = 0;
////            int hits = 0;
////            int startIndex = 0;
////            double pattFreq;
////            double sum;
////            for(double nowFreq : Frequeue) {
////                pattFreq = sp[i++];
////                if(nowFreq - readSnd.getPatternTolerance() <= pattFreq && pattFreq <= nowFreq + readSnd.getPatternTolerance()) {
////                    hits += 1;
////                }
////            }
////            // 백분율로 판단
////            double similarPercent = hits / readSnd.getPatternMaxCnt() * 100.0F;
////            Log.d("TAG", "아기울음 히트수 = " +  hits);
////            if( similarPercent > 50.0F) {
////                Log.d("TAG", "아기울음 유사도 = " +  similarPercent);
////            }
////-----------------------------------------------------
//// Method 2
//// 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
////            Point[] pattenPoint = new Point[sp.length];
//            Point[] pattenPoint = new Point[sp.size()];
//            Point[] targetPoint = new Point[Frequeue.size()];
//            int i = 0;
//            for(double nowFreq : Frequeue) {
//                pattenPoint[i] = new Point();
//                targetPoint[i] = new Point();
//                pattenPoint[i].x = i;
////                pattenPoint[i].y = (int)sp[i];
//                pattenPoint[i].y = sp.get(i).intValue();
//                targetPoint[i].x = i;
//                targetPoint[i].y = (int)nowFreq;
//                i++;
//            }
//            float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
//            if(dist < APPROXIMATE) {
//                //Log.d("TAG", "아기울음 근접도 HIGH = " + dist);
//                if(myRecg.boMode_1 == false) {
//                    myRecg.boMode_1_cnt++;
//                    if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
//                        final String text = "아기가 울고 있습니다.";
////                        mainActivity.showToast(text);
//                        mainActivity.runOnUiThread(new Runnable() {
//                            public void run()
//                            {
//                                Toast.makeText(mainActivity, text, Toast.LENGTH_SHORT).show();
//
//                                jjHistory history = new jjHistory("angelband", "bandID", text, "contents", "location", "mic", "noise", "date", "1", "2", "3");
//                                mDBHelper = new jjDBHelper(mainActivity);
//                                mDBHelper.openDatabase();
//                                mDBHelper.addHistory(history);
//                            }
//                        });
//                        myRecg.boMode_1_cnt = 0;
//                        myRecg.boMode_1 = false;
//                    }
//                }
//            }
//            else {
////                Log.d("TAG", ">>>>>>>아기울음 근접도 낮음 = " + dist);
//            }
////            if(dist < minDist) {
////                Log.d("TAG", "아기울음 근접도= " + minDist);
////
////                minDist = dist;
////            }
//            // 마지막 큐의 내용을 꺼내 지운다.
//            Frequeue.poll();
//        }
//    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////

//    void calculatePeak() {
//        getSpectrumAmpDB();
//        // Find and show peak amplitude
//        maxAmpDB  = 20 * log10(0.125/32768);
//        maxAmpFreq = 0;
//        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
//            if (spectrumAmpOutDB[i] > maxAmpDB) {
//                maxAmpDB  = spectrumAmpOutDB[i];
//                maxAmpFreq = i;
//            }
//        }
//
//        // Slightly better peak finder
//        // The peak around spectrumDB should look like quadratic curve after good window function
//        // a*x^2 + b*x + c = y
//        // a - b + c = x1
//        //         c = x2
//        // a + b + c = x3
//        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
//            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
//            double x1 = spectrumAmpOutDB[id-1];
//            double x2 = spectrumAmpOutDB[id];
//            double x3 = spectrumAmpOutDB[id+1];
//            double c = x2;
//            double a = (x3+x1)/2 - x2;
//            double b = (x3-x1)/2;
//            if (a < 0) {
//                double xPeak = -b/(2*a);
//                if (abs(xPeak) < 1) {
//                    maxAmpFreq += xPeak * sampleRate / fftLen;
//                    maxAmpDB = (4*a*c - b*b)/(4*a);
//                }
//            }
//        }
//
//        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//        Log.d("TAG", "아기울음 진동수 = " +  maxAmpFreq);
//        Frequeue.offer(maxAmpFreq);
//
//        for (int idx = 0; idx < readSnd.getSoundPatternArr().size(); idx++) {
//            if(Frequeue.size() >= readSnd.getPatternMaxCnt(idx)) {  // 최초에만 비어 있고 그 다음에는 계속 55 개 이다..그러므로 항상 실시간으로 검사한다.
//                ArrayList<Double> sp = readSnd.getSoundPattern(idx);
//// Method 2
//// 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
////            Point[] pattenPoint = new Point[sp.length];
//                Point[] pattenPoint = new Point[sp.size()];
//                Point[] targetPoint = new Point[Frequeue.size()];
//                int i = 0;
//                for(double nowFreq : Frequeue) {
//                    pattenPoint[i] = new Point();
//                    targetPoint[i] = new Point();
//                    pattenPoint[i].x = i;
////                pattenPoint[i].y = (int)sp[i];
//                    pattenPoint[i].y = sp.get(i).intValue();
//                    targetPoint[i].x = i;
//                    targetPoint[i].y = (int)nowFreq;
//                    i++;
//                }
//                float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
//                Log.d("TAG", "dist / APPROXIMATE =" + dist+ " / "+ APPROXIMATE);
//                if(dist < APPROXIMATE) {
//                    Log.d("TAG", "cnt: " + myRecg.boMode_1_cnt +" 아기울음 근접도 HIGH = " + dist);
//                    if(myRecg.boMode_1 == false) {
//                        myRecg.boMode_1_cnt++;
//                        if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
//                            final String text = "아기가 울고 있습니다.";
//                            mainActivity.showToast(text);
//                            myRecg.boMode_1_cnt = 0;
//                            myRecg.boMode_1 = false;
//                        }
//                    }
//                }
//                else {
////                Log.d("TAG", ">>>>>>>아기울음 근접도 낮음 = " + dist);
//                }
//
//                // 마지막 큐의 내용을 꺼내 지운다.
//                Frequeue.poll();
//            }
//
//        }
//
//    }

////////////////////////////////////////////////////////////////////////////////////////////////////

//    void calculatePeak() {
//        getSpectrumAmpDB();
//        // Find and show peak amplitude
//        maxAmpDB  = 20 * log10(0.125/32768);
//        maxAmpFreq = 0;
//        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
//            if (spectrumAmpOutDB[i] > maxAmpDB) {
//                maxAmpDB  = spectrumAmpOutDB[i];
//                maxAmpFreq = i;
//            }
//        }
//
//        // Slightly better peak finder
//        // The peak around spectrumDB should look like quadratic curve after good window function
//        // a*x^2 + b*x + c = y
//        // a - b + c = x1
//        //         c = x2
//        // a + b + c = x3
//        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
//            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
//            double x1 = spectrumAmpOutDB[id-1];
//            double x2 = spectrumAmpOutDB[id];
//            double x3 = spectrumAmpOutDB[id+1];
//            double c = x2;
//            double a = (x3+x1)/2 - x2;
//            double b = (x3-x1)/2;
//            if (a < 0) {
//                double xPeak = -b/(2*a);
//                if (abs(xPeak) < 1) {
//                    maxAmpFreq += xPeak * sampleRate / fftLen;
//                    maxAmpDB = (4*a*c - b*b)/(4*a);
//                }
//            }
//        }
//
//        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//        //Log.d("TAG", "아기울음 진동수 = " +  maxAmpFreq);
//        Frequeue.offer(maxAmpFreq);
//
//        for (int idx = 0; idx < readSnd.getSoundPatternArr().size(); idx++) {
//            if(Frequeue.size() >= readSnd.getPatternMaxCnt(idx)) {  // 최초에만 비어 있고 그 다음에는 계속 55 개 이다..그러므로 항상 실시간으로 검사한다.
//                ArrayList<Double> sp = readSnd.getSoundPattern(idx);
//// Method 2
//// 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
////            Point[] pattenPoint = new Point[sp.length];
//                Point[] pattenPoint = new Point[sp.size()];
//                Point[] targetPoint = new Point[Frequeue.size()];
//                int i = 0;
//                for(double nowFreq : Frequeue) {
//                    pattenPoint[i] = new Point();
//                    targetPoint[i] = new Point();
//                    pattenPoint[i].x = i;
////                pattenPoint[i].y = (int)sp[i];
//                    pattenPoint[i].y = sp.get(i).intValue();
//                    targetPoint[i].x = i;
//                    targetPoint[i].y = (int)nowFreq;
//                    i++;
//                }
//                float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
//                //Log.d("TAG", "dist / APPROXIMATE =" + dist+ " / "+ APPROXIMATE);
//                if(dist < APPROXIMATE) {
//                    //Log.d("TAG", "cnt: " + myRecg.boMode_1_cnt +" 아기울음 근접도 HIGH = " + dist);
////                    if(myRecg.boMode_1 == false) {
////                        myRecg.boMode_1_cnt++;
////                        if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
////                            final String text = "아기가 울고 있습니다.";
////                            mainActivity.showToast(text);
////                            myRecg.boMode_1_cnt = 0;
////                            myRecg.boMode_1 = false;
////                        }
////                    }
//
//                    //예외
//                    for (int j = 0; j < readSnd.getExSoundPatternArr().size(); j++) {
//                        if (Frequeue.size() >= readSnd.getExPatternMaxCnt(j)) {
//                            ArrayList<Double> exArr = readSnd.getExSoundPattern(j);
//                            //Log.d("TAG", "예외 "+j);
//
////                            Point[] pattenPointt = new Point[exArr.size()];
////                            Point[] targetPointt = new Point[Frequeue.size()];
////                            int k = 0;
////                            for(double nowFreq : Frequeue) {
////                                pattenPointt[k] = new Point();
////                                targetPointt[k] = new Point();
////                                pattenPointt[k].x = k;
////                                pattenPointt[k].y = exArr.get(k).intValue();
////                                targetPointt[k].x = k;
////                                targetPointt[k].y = (int)nowFreq;
////                                k++;
////                            }
////                            float distt = jjSoundCloudMatch( targetPointt, pattenPointt );
////                            if(distt > APPROXIMATE) {
////                                Log.d("TAG", "예외  distt < APPROXIMATE ");
//////                                if(myRecg.boMode_1 == false) {
//////                                    myRecg.boMode_1_cnt++;
//////                                    if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
//////                                        final String text = "아기가 울고 있습니다.";
//////                                        mainActivity.showToast(text);
//////                                        myRecg.boMode_1_cnt = 0;
//////                                        myRecg.boMode_1 = false;
//////                                    }
//////                                }
////
////                            }
//                        }
//                    }
//
//                }
//                else {
////                Log.d("TAG", ">>>>>>>아기울음 근접도 낮음 = " + dist);
//                }
//
//                // 마지막 큐의 내용을 꺼내 지운다.
//                Frequeue.poll();
//            }
//
//        }
//
//    }

    void calculatePeak() {
        getSpectrumAmpDB();
        // Find and show peak amplitude
        maxAmpDB  = 20 * log10(0.125/32768);
        maxAmpFreq = 0;
        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
            if (spectrumAmpOutDB[i] > maxAmpDB) {
                maxAmpDB  = spectrumAmpOutDB[i];
                maxAmpFreq = i;
            }
        }

        // Slightly better peak finder
        // The peak around spectrumDB should look like quadratic curve after good window function
        // a*x^2 + b*x + c = y
        // a - b + c = x1
        //         c = x2
        // a + b + c = x3
        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
            double x1 = spectrumAmpOutDB[id-1];
            double x2 = spectrumAmpOutDB[id];
            double x3 = spectrumAmpOutDB[id+1];
            double c = x2;
            double a = (x3+x1)/2 - x2;
            double b = (x3-x1)/2;
            if (a < 0) {
                double xPeak = -b/(2*a);
                if (abs(xPeak) < 1) {
                    maxAmpFreq += xPeak * sampleRate / fftLen;
                    maxAmpDB = (4*a*c - b*b)/(4*a);
                }
            }
        }

        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
        Log.d("TAG", "아기울음 진동수 = " +  maxAmpFreq);
        Frequeue.offer(maxAmpFreq);

        //큐를 고정으로 하고 비교 해보자....
        if(Frequeue.size() >= 50) {

//            for (int k = 0; k < readSnd.getBellSoundPatternArr().size(); k++) {
//                int minSize = 0;
//                minSize = min(Frequeue.size(), readSnd.getBellPatternMaxCnt(k));
//                {
//                    ArrayList<Double> arr = readSnd.getBellSoundPattern(k);
//
//                    // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                    Point[] pattenPoint = new Point[arr.size()];
//                    Point[] targetPoint = new Point[Frequeue.size()];
//                    int i = 0;
//                    for(double nowFreq : Frequeue) {
//                        if (i >= minSize) {
//                        } else {
//                            pattenPoint[i] = new Point();
//                            targetPoint[i] = new Point();
//                            pattenPoint[i].x = i;
//                            pattenPoint[i].y = arr.get(i).intValue();
//                            targetPoint[i].x = i;
//                            targetPoint[i].y = (int)nowFreq;
//                        }
//                        i++;
//                    }
//
//                    float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
//                    if(dist < BELLAPPROXIMATE) {
//                        //Log.d("TAG", "아기울음 근접도 HIGH = " + dist);
//                        if(myRecg.boMode_1 == false) {
//                            myRecg.boMode_1_cnt++;
//                            if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
//                                final String text = "벨이 울리고 있습니다.";
//                                int idx = 0;
//                                mainActivity.showToast(text, idx);
//                                myRecg.boMode_1_cnt = 0;
//                                myRecg.boMode_1 = false;
//                                sgl.peakSound = true;
//                                sgl.setNowTime();
//                                mainActivity.setBTSoc(true);
//
//                                byte[] data = new byte[1];
//                                data[0] = 0x1d;
//                                sgl.sendBTNotice(data);
//
//                                continue;
//                            }
//                        }
//
//                    }
//
//                }
//            } // 벨 패턴 검색
//
//            for (int j = 0; j < readSnd.getSoundPatternArr().size(); j++) {
//                int minSize = 0;
//                minSize = min(Frequeue.size(), readSnd.getPatternMaxCnt(j));
//                {
//                    ArrayList<Double> arr = readSnd.getSoundPattern(j);
//
//                    // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                    Point[] pattenPoint = new Point[arr.size()];
//                    Point[] targetPoint = new Point[Frequeue.size()];
//                    int i = 0;
//                    for(double nowFreq : Frequeue) {
//                        if (i >= minSize) {
//                        } else {
//                            pattenPoint[i] = new Point();
//                            targetPoint[i] = new Point();
//                            pattenPoint[i].x = i;
//                            pattenPoint[i].y = arr.get(i).intValue();
//                            targetPoint[i].x = i;
//                            targetPoint[i].y = (int)nowFreq;
//                        }
//                        i++;
//                    }
//
//                    float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
//                    if(dist < APPROXIMATE) {
//                        //Log.d("TAG", "아기울음 근접도 HIGH = " + dist);
//                        if(myRecg.boMode_1 == false) {
//                            myRecg.boMode_1_cnt++;
//                            if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
//                                final String text = "아기가 울고 있습니다.";
//                                int idx = 1;
//                                mainActivity.showToast(text, idx);
//                                myRecg.boMode_1_cnt = 0;
//                                myRecg.boMode_1 = false;
//                                sgl.peakSound = true;
//                                sgl.setNowTime();
//                                mainActivity.setBTSoc(true);
//
//                                byte[] data = new byte[1];
//                                data[0] = 0x1a;
//                                sgl.sendBTNotice(data);
//
//                                continue;
//                            }
//                        }
//
//                    }
//
//                }
//            } // 울음 패턴 검색 끝

            for (int j = 0; j < readSnd.getFireSoundPatternArr().size(); j++) {
                int minSize = 0;
                minSize = min(Frequeue.size(), readSnd.getFirePatternMaxCnt(j));
                {
                    ArrayList<Double> arr = readSnd.getFireSoundPattern(j);

                    // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
                    Point[] pattenPoint = new Point[arr.size()];
                    Point[] targetPoint = new Point[Frequeue.size()];
                    int i = 0;
                    for(double nowFreq : Frequeue) {
                        if (i >= minSize) {
                        } else {
                            pattenPoint[i] = new Point();
                            targetPoint[i] = new Point();
                            pattenPoint[i].x = i;
                            pattenPoint[i].y = arr.get(i).intValue();
                            targetPoint[i].x = i;
                            targetPoint[i].y = (int)nowFreq;
                        }
                        i++;
                    }

                    float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
                    if(dist < FIREAPPROXIMATE) {
                        //Log.d("TAG", "아기울음 근접도 HIGH = " + dist);
                        if(myRecg.boMode_1 == false) {
                            myRecg.boMode_1_cnt++;
                            if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
                                final String text = "사이렌이 울리고 있습니다.";
                                int idx = 2;
                                mainActivity.showToast(text, idx);
                                myRecg.boMode_1_cnt = 0;
                                myRecg.boMode_1 = false;
                                sgl.peakSound = true;
                                sgl.setNowTime();
                                mainActivity.setBTSoc(true);

                                byte[] data = new byte[1];
                                data[0] = 0x1b;
                                sgl.sendBTNotice(data);

                                continue;
                            }
                        }

                    }

                }
            } // 사이렌 패턴 검색 끝

            if (sgl.mode1 == true) {

            }

            // 마지막 큐의 내용을 꺼내 지운다.
            Frequeue.poll();
        }
    }

    float calculatePeakBell() {
        getSpectrumAmpDB();
        float sDist = 999999999;
        // Find and show peak amplitude
        maxAmpDB  = 20 * log10(0.125/32768);
        maxAmpFreq = 0;
        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
            if (spectrumAmpOutDB[i] > maxAmpDB) {
                maxAmpDB  = spectrumAmpOutDB[i];
                maxAmpFreq = i;
            }
        }

        // Slightly better peak finder
        // The peak around spectrumDB should look like quadratic curve after good window function
        // a*x^2 + b*x + c = y
        // a - b + c = x1
        //         c = x2
        // a + b + c = x3
        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
            double x1 = spectrumAmpOutDB[id-1];
            double x2 = spectrumAmpOutDB[id];
            double x3 = spectrumAmpOutDB[id+1];
            double c = x2;
            double a = (x3+x1)/2 - x2;
            double b = (x3-x1)/2;
            if (a < 0) {
                double xPeak = -b/(2*a);
                if (abs(xPeak) < 1) {
                    maxAmpFreq += xPeak * sampleRate / fftLen;
                    maxAmpDB = (4*a*c - b*b)/(4*a);
                }
            }
        }

        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//        Log.d("TAG", "아기울음 진동수 = " +  maxAmpFreq);
        Frequeue.offer(maxAmpFreq);

        //큐를 고정으로 하고 비교 해보자....
        if(Frequeue.size() >= 50) {

            for (int k = 0; k < readSnd.getBellSoundPatternArr().size(); k++) {
                int minSize = 0;
                minSize = min(Frequeue.size(), readSnd.getBellPatternMaxCnt(k));
                {
                    ArrayList<Double> arr = readSnd.getBellSoundPattern(k);

                    // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
                    Point[] pattenPoint = new Point[arr.size()];
                    Point[] targetPoint = new Point[Frequeue.size()];
                    int i = 0;
                    for(double nowFreq : Frequeue) {
                        if (i >= minSize) {
                        } else {
                            pattenPoint[i] = new Point();
                            targetPoint[i] = new Point();
                            pattenPoint[i].x = i;
                            pattenPoint[i].y = arr.get(i).intValue();
                            targetPoint[i].x = i;
                            targetPoint[i].y = (int)nowFreq;
                        }
                        i++;
                    }

                    float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
//                    Log.d("TAG", "bell 근접도 HIGH = " + dist);
                    if(dist < BELLAPPROXIMATE) {
                        if(myRecg.boMode_1 == false) {
                            myRecg.boMode_1_cnt++;
                            if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
                                myRecg.boMode_1_cnt = 0;
                                myRecg.boMode_1 = false;

                                int result = compare(sDist, 999999999);
                                if( result == 0) {
                                    sDist = dist;
                                }else {
                                    sDist = Math.min(dist, sDist);
                                }

                            }
                        }

                    }

//                    int result = compare(dist, BELLAPPROXIMATE);
//
//                    if( result == 0) {
//                        Log.d("bell_compare", "Equals");
//                    } else if( result < 0 ) {
//                        Log.d("bell_compare", "dist < BELLAPPROXIMATE");
//                    } else {
//                        Log.d("bell_compare", "dist > BELLAPPROXIMATE");
//                    }


                }
            } // 벨 패턴 검색

            if (sgl.mode1 == true) {

            }

            // 마지막 큐의 내용을 꺼내 지운다.
            Frequeue.poll();
        }

        return sDist;
    }


    float calculatePeakBaby() {
        getSpectrumAmpDB();
        float sDist = 999999999;
        // Find and show peak amplitude
        maxAmpDB  = 20 * log10(0.125/32768);
        maxAmpFreq = 0;
        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
            if (spectrumAmpOutDB[i] > maxAmpDB) {
                maxAmpDB  = spectrumAmpOutDB[i];
                maxAmpFreq = i;
            }
        }

        // Slightly better peak finder
        // The peak around spectrumDB should look like quadratic curve after good window function
        // a*x^2 + b*x + c = y
        // a - b + c = x1
        //         c = x2
        // a + b + c = x3
        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
            double x1 = spectrumAmpOutDB[id-1];
            double x2 = spectrumAmpOutDB[id];
            double x3 = spectrumAmpOutDB[id+1];
            double c = x2;
            double a = (x3+x1)/2 - x2;
            double b = (x3-x1)/2;
            if (a < 0) {
                double xPeak = -b/(2*a);
                if (abs(xPeak) < 1) {
                    maxAmpFreq += xPeak * sampleRate / fftLen;
                    maxAmpDB = (4*a*c - b*b)/(4*a);
                }
            }
        }

        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
        //Log.d("TAG", "아기울음 진동수 = " +  maxAmpFreq);
        Frequeue.offer(maxAmpFreq);

        //큐를 고정으로 하고 비교 해보자....
        if(Frequeue.size() >= 50) {

            for (int j = 0; j < readSnd.getSoundPatternArr().size(); j++) {
                int minSize = 0;
                minSize = min(Frequeue.size(), readSnd.getPatternMaxCnt(j));
                {
                    ArrayList<Double> arr = readSnd.getSoundPattern(j);

                    // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
                    Point[] pattenPoint = new Point[arr.size()];
                    Point[] targetPoint = new Point[Frequeue.size()];
                    int i = 0;
                    for(double nowFreq : Frequeue) {
                        if (i >= minSize) {
                        } else {
                            pattenPoint[i] = new Point();
                            targetPoint[i] = new Point();
                            pattenPoint[i].x = i;
                            pattenPoint[i].y = arr.get(i).intValue();
                            targetPoint[i].x = i;
                            targetPoint[i].y = (int)nowFreq;
                        }
                        i++;
                    }

                    float dist = jjSoundCloudMatch( targetPoint, pattenPoint );
//                    Log.d("TAG", "baby 근접도 HIGH = " + dist);
                    if(dist < APPROXIMATE) {
                        if(myRecg.boMode_1 == false) {
                            myRecg.boMode_1_cnt++;
                            if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
                                myRecg.boMode_1_cnt = 0;
                                myRecg.boMode_1 = false;

                                int result = compare(sDist, 999999999);
                                if( result == 0) {
                                    sDist = dist;
                                }else {
                                    sDist = Math.min(dist, sDist);
                                }

                            }
                        }
                    }


                }
            } // 울음 패턴 검색 끝

            if (sgl.mode1 == true) {

            }

            // 마지막 큐의 내용을 꺼내 지운다.
            Frequeue.poll();
        }

        return sDist;
    }

    boolean calculatePeakFire() {
        getSpectrumAmpDB();
        boolean retVal = false;
        // Find and show peak amplitude
        maxAmpDB  = 20 * log10(0.125/32768);
        maxAmpFreq = 0;
        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
            if (spectrumAmpOutDB[i] > maxAmpDB) {
                maxAmpDB  = spectrumAmpOutDB[i];
                maxAmpFreq = i;
            }
        }

        // Slightly better peak finder
        // The peak around spectrumDB should look like quadratic curve after good window function
        // a*x^2 + b*x + c = y
        // a - b + c = x1
        //         c = x2
        // a + b + c = x3
        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
            double x1 = spectrumAmpOutDB[id-1];
            double x2 = spectrumAmpOutDB[id];
            double x3 = spectrumAmpOutDB[id+1];
            double c = x2;
            double a = (x3+x1)/2 - x2;
            double b = (x3-x1)/2;
            if (a < 0) {
                double xPeak = -b/(2*a);
                if (abs(xPeak) < 1) {
                    maxAmpFreq += xPeak * sampleRate / fftLen;
                    maxAmpDB = (4*a*c - b*b)/(4*a);
                }
            }
        }

        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
        Log.d("TAG", "사이렌 진동수 = " +  maxAmpFreq);
        Frequeue.offer(maxAmpFreq);

        //큐를 고정으로 하고 비교 해보자....
        if(Frequeue.size() >= 10) {

            if (sgl.mode1 == true) {

            }

            double total = 0;
            for (double nowFreq : Frequeue) {
                total = total + (int)nowFreq;
            }
            double avgFreq = total / Frequeue.size();
            Log.d("tag", "평균 진동수 : " + avgFreq);

            if(avgFreq > 1200) {
                //Log.d("TAG", "아기울음 근접도 HIGH = " + dist);
                if(myRecg.boMode_1 == false) {
                    myRecg.boMode_1_cnt++;
                    if(myRecg.boMode_1_cnt > myRecg.MAX_DICISION_CNT) {
                        final String text = "화재경보가 울리고 있습니다.";
                        int idx = 2;
                        mainActivity.showToast(text, idx);
                        myRecg.boMode_1_cnt = 0;
                        myRecg.boMode_1 = false;
                        sgl.peakSound = true;
                        sgl.setNowTime();
                        mainActivity.setBTSoc(true);

                        byte[] data = new byte[1];
                        data[0] = 0x1b;
                        sgl.sendBTNotice(data);

                        retVal = true;
                    }
                }

            }

            // 마지막 큐의 내용을 꺼내 지운다.
            Frequeue.poll();
        }

        return retVal;
    }

    void counterReset()
    {
        myRecg.boMode_fire_cnt = 0;
        myRecg.boMode_baby_cnt = 0;
        myRecg.boMode_bell_cnt = 0;
        myRecg.boMode_etcc_cnt = 0;
        dropCnt = 0;
        midCnt = 0;
        upCnt = 0;
    }

    public static double mean(Queue<Double> Frequeue) {  // 산술 평균 구하기
        double sum = 0.0;

        for (double nowFreqY : Frequeue) {
            sum += nowFreqY;
        }

        return sum / Frequeue.size();
    }

    //
    // 표준편차 구하기..분산도 측정..
    //
    public static double standardDeviation(Queue<Double> Frequeue, int option) {
        if (Frequeue.size() < 2) {
            return Double.NaN;
        }

        double sum = 0.0;
        double sd = 0.0;
        double diff;
        double meanValue = mean(Frequeue);

        for (double nowFreqY : Frequeue) {
            diff = nowFreqY - meanValue;
            sum += diff * diff;
        }
        sd = Math.sqrt(sum / (Frequeue.size() - option));

        return sd;
    }

    // 1. 화재경보는 특정 진동수의 기울기로 판단한다.
    // 패턴 1 : 3400 Hz : 기울기 0
    // 패턴 2 : 3200 Hz : 기울기 0
    // 패턴 3 : 2600 Hz : 기울기 0
    // 패턴 4 : 1400 Hz : 기울기 0
    void decideWhatKindOfSound(double rmsdB, boolean dropCheck) {

        getSpectrumAmpDB();
//        boolean retVal = false;
        // Find and show peak amplitude
        maxAmpDB  = 20 * log10(0.125/32768.0);
        maxAmpFreq = 0;
        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
            if (spectrumAmpOutDB[i] > maxAmpDB) {
                maxAmpDB  = spectrumAmpOutDB[i];
                maxAmpFreq = i;
            }
        }

        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
            double x1 = spectrumAmpOutDB[id-1];
            double x2 = spectrumAmpOutDB[id];
            double x3 = spectrumAmpOutDB[id+1];
            double c = x2;
            double a = (x3+x1)/2 - x2;
            double b = (x3-x1)/2;
            if (a < 0) {
                double xPeak = -b/(2*a);
                if (abs(xPeak) < 1) {
                    maxAmpFreq += xPeak * sampleRate / fftLen;
                    maxAmpDB = (4*a*c - b*b)/(4*a);
                }
            }
        }

        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//        Log.d("TAG", "maxAmpFreq 진동수 = " +  maxAmpFreq + " maxAmpDB = " + maxAmpDB);

//        mainActivity.showDecibel(0);    // 데시벨 표시

//        curFloorAmpFreq = Math.floor(maxAmpFreq/100.0F) *100.0F;
////        curFloorAmpFreq = curFloorAmpFreq - (curFloorAmpFreq%firePatternFreq[0]);
        if(maxAmpFreq > 1000.0F) {
//            Log.d("TAG", "maxAmpFreq 진동수 = " + maxAmpFreq);
        }
//        // 진돌수가 화재 패턴 1번과 100단위로 같은 값이 연속으로 들어오면 화재경보로 판단.
//        if((int)curFloorAmpFreq == (int)firePatternFreq[0]) {
//            fireFrequeue34.offer(curFloorAmpFreq);
//        }
//        else if((int)curFloorAmpFreq == (int)firePatternFreq[1]) {
//            fireFrequeue32.offer(curFloorAmpFreq);
//        }
//        else if((int)curFloorAmpFreq == (int)firePatternFreq[2]) {
//            fireFrequeue26.offer(curFloorAmpFreq);
//        }
//        else if((int)curFloorAmpFreq == (int)firePatternFreq[3]) {
//            fireFrequeue14.offer(curFloorAmpFreq);
//        }
        Frequeue.offer(maxAmpFreq);

        //큐를 고정으로 하고 비교 해보자....
        if(Frequeue.size() >= 50) {
            // 1. 음악이나, 사이렌은 연속음이기에 dB이 떨어지는 구간이 없다.
            //    따라서 dB이 떨어지는 구간이 있으면, 그것은 음악이나 사이렌이 아니다.
            // 2. 진동수는 사이렌도 그렇게 높지 않은 것도 있으므로 진동수로 기준을 잡아서는 안된다.
            // 3. 일단 실시간으로 아기울음이나 초인종인지를 계속 판단하고 있되, 3초가 되기 전에는 결정을 하지 않는다.

            float bellDist = 999999999;
            float babyDist = 999999999;
            // 아기울음의 표준편차가 가장 크다.(즉 모든 진동수가 분산되어 분산도가 높다.)
            // 음악의 표준편차는 대략 300~600 사이이다.
            // 아기 울음의 표준편차는 600 을 넘는다..

            double sdVal = standardDeviation(Frequeue, 1);
            int isVal = (int)sdVal;
            if(isVal >= 300 && isVal <= 600) {
                myRecg.boMode_etcc_cnt++;
            }

            // 먼저 50개 진동수내에 연속으로 화재경보 진동수 값이 들어오는지 확인한다.
            for (double fireFreq : firePatternFreq) {
                int i = 0;
                int sameCnt = 0;
                double preFloorFreqY = 0.0F;
//                Log.d("TAG", "firePatternFreq 진동수 = " + fireFreq );
                for (double nowFreqY : Frequeue) {
                    // 100 단위는 절사..
                    curFloorAmpFreq = Math.floor(Math.round(nowFreqY)/100.0F) *100.0F;
                    curFloorAmpFreq = curFloorAmpFreq - (curFloorAmpFreq%fireFreq);
                    int aSide = (int)curFloorAmpFreq;
                    int bSide = (int)fireFreq;
                    if(curFloorAmpFreq != fireFreq) {
                       // Log.d("TAG", "curFloorAmpFreq 진동수 불일치 = " + curFloorAmpFreq + "/fireFreq = " + fireFreq);
                        continue;
                    }
                    else {
//                        Log.d("TAG", "curFloorAmpFreq 진동수 일치 = " + aSide + "/bSide = " + bSide);
                    }
                    if( i == 0) {
                        preFloorFreqY = curFloorAmpFreq;
                    }
                    else {
                        if((int)preFloorFreqY == (int)curFloorAmpFreq) {
                            preFloorFreqY = curFloorAmpFreq;
                            sameCnt++;
                            if(sameCnt >= myRecg.MAX_FIREDICISION_SAME_CNT) {    // 10번이상 연속으로 같은 진동수가 들어오면 화재경보이다.
                                myRecg.boMode_fire_cnt++;
                                break;
                            }
                        }
                        else {
//                            preFloorFreqY = 0.0F;
//                            sameCnt = 0;
//                            i = 0;
//                            break;
                        }
                    }
                    i++;
                }
            }

            // 4. 먼저 아기울음 패턴과 비교하여 점수를 매긴다.
            //루프가 너무 느리다 돌때 들어오는 소리들을 감지하지 못한다.
            for (ArrayList<Double> arr : readSnd.getSoundPatternArr()) {
                int minSize = min(Frequeue.size(), arr.size());
                // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
                Point[] pattenPoint = new Point[arr.size()];
                Point[] targetPoint = new Point[Frequeue.size()];
                int i = 0;
                for (double nowFreq : Frequeue) {
                    if (i >= minSize) {
                    } else {
                        pattenPoint[i] = new Point();
                        targetPoint[i] = new Point();
                        pattenPoint[i].x = i;
                        pattenPoint[i].y = arr.get(i).intValue();
                        targetPoint[i].x = i;
                        targetPoint[i].y = (int) nowFreq;
                    }
                    i++;
                }

                float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
                //Log.d("TAG", "아기울음 패턴 최소거리 = " +  dist);
                    if (dist < APPROXIMATE) {

                        myRecg.boMode_baby_cnt++;
                        if (myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT) {
                            //myRecg.boMode_baby_cnt = 0;
                            babyDist = dist;
                        }
                    }

                //평균값을 구하기위해 전체 값을 구한다
                sgl.babyTotalDist = sgl.babyTotalDist + dist;
                sgl.babyDistCnt++;

            }

            // 5. 다음 초인종 패턴과 비교하여 점수를 매긴다.

            for (ArrayList<Double> arr : readSnd.getBellSoundPatternArr()) {
                int minSize = min(Frequeue.size(), arr.size());
                // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
                Point[] pattenPoint = new Point[arr.size()];
                Point[] targetPoint = new Point[Frequeue.size()];
                int i = 0;
                for (double nowFreq : Frequeue) {
                    if (i >= minSize) {
                    } else {
                        pattenPoint[i] = new Point();
                        targetPoint[i] = new Point();
                        pattenPoint[i].x = i;
                        pattenPoint[i].y = arr.get(i).intValue();
                        targetPoint[i].x = i;
                        targetPoint[i].y = (int) nowFreq;
                    }
                    i++;
                }

                float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
                    if (dist < BELLAPPROXIMATE) {

                        myRecg.boMode_bell_cnt++;
                        if (myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT) {
                            //myRecg.boMode_bell_cnt = 0;
                            bellDist = dist;
                        }
                    }

                //평균값을 구하기위해 전체 값을 구한다
//                sgl.bellTotalDist = sgl.bellTotalDist +dist;
//                sgl.bellDistCnt++;
            }
            //
            //     6. 전체 3개 유형 비교한다.
            //     - 그러나 일반 소음을 먼저 구분해야 한다.
            //
            //  먼저 일반적인 소음의 특징을 잡는다. 특히 음악 등은 온갖 진동수의 소리가 동시에 다 들어오며
            // 특정한 dB 이상의 소리가 지속적으로 들어온다.(이것이 분간하기 매우 어렵다..)


//            int rmsResult = compare((float)rmsdB, DROP_DECIBEL);
//            if(rmsResult < 0 ) {    // 40dB 이 조용할때 암소음의 dB임(즉 소리가 없을 때)..그러나 현실은 다름..
//                dropCnt++;
//            }
//            int rmsMResult = compare((float)rmsdB, MID_DECIBEL);
//            if(rmsMResult > 0 ) {    // 60dB 을 보통 말소리 데시벨로 가정
//                midCnt++;
//            }
//            int rmsHResult = compare((float)rmsdB, HIGH_DECIBEL);
//            if(rmsHResult > 0 ) {    // 70dB 을 화재경보의 데시벨로 가정
//                upCnt++;
//            }
//            float updownRate = dropCnt/midCnt;
//
//            int cmpResult = compare((float)sgl.oldRmsdB, 0);
//            if (cmpResult != 0) {
//                double grad = sgl.oldRmsdB - rmsdB;
//                Log.d("TAG", "grad? = "+grad);
//            }

            if( sgl.startMonitor == false) {// 최초 감시 시작단계에서는 결정을 내리지 않는다.
                sgl.startMonitor = true;
                counterReset();
                Frequeue.poll();
                return;
            }
            if(dropCheck == false) { // 3초 이내에는 결정을 내리지 않는다.(사이렌이나 음악소리 등 연속음일 수 있음)
//                여기서 리셋하면 절대 안된다. 이부분은 3초내에 수십번 도는 곳이다.
//                myRecg.boMode_fire_cnt = 0;
//                myRecg.boMode_baby_cnt = 0;
//                myRecg.boMode_bell_cnt = 0;

                Frequeue.poll();
//                fireFrequeue.poll();
                return;
            }

            Log.d("TAG", "3초가 지나 결정시기가 옴.....표준편차 = " + sdVal );
//            Log.d("TAG", "3초가 지나 결정시기가 옴.....dropCnt = " + dropCnt + " upCnt = " + upCnt + " midCnt = " + midCnt);
//            sgl.babyAvgDist = sgl.babyTotalDist/sgl.babyDistCnt;
//            sgl.bellAvgDist = sgl.bellTotalDist/sgl.bellDistCnt;
//            sgl.babyAvgDist = sgl.babyTotalDist/myRecg.boMode_baby_cnt;
//            sgl.bellAvgDist = sgl.bellTotalDist/myRecg.boMode_bell_cnt;
//            sgl.babyTotalDist = sgl.bellTotalDist = sgl.babyDistCnt = sgl.bellDistCnt = 0;
            // 암소음 구간이 3초동안 10회보다 작으MAX_FIREDICISION_CNT면서, 80dB 이상 구간이 10회보다 크면 사이렌이나 음악소리이다.
            // 기울기가 0 이면서 특정 진동수 대역이면 화재경보이다.

//          if ( dropCnt < myRecg.MAX_FIREDICISION_CNT  && upCnt > myRecg.MAX_FIREDICISION_CNT)
//            if(fireFrequeue34.size() >= 3 ||
//               fireFrequeue32.size() >= 3 ||
//               fireFrequeue26.size() >= 3 ||
//               fireFrequeue14.size() >= 3
//                    ) // 3초동안 5번 이상 같은 진동수가 들어오면..화재경보로 판단..
            Log.d("TAG", "3초 지남 = " + myRecg.boMode_fire_cnt + "/" + myRecg.boMode_baby_cnt + "/" + myRecg.boMode_bell_cnt + "/" +  myRecg.boMode_etcc_cnt );
            int maxVal = Math.max(myRecg.boMode_fire_cnt, myRecg.boMode_baby_cnt);
            maxVal = Math.max(maxVal, myRecg.boMode_bell_cnt);
            maxVal = Math.max(maxVal, myRecg.boMode_etcc_cnt);
            if(maxVal != 0 ) {
                if (myRecg.boMode_fire_cnt == maxVal && myRecg.boMode_etcc_cnt < 5 && sdVal > 1000) { // 분산이 안되면서 화재경보가 울려야 한다.
                    dropCnt = 0;
                    midCnt = 0;
                    upCnt = 0;

                    final String text = "화재경보가 울리고 있습니다.";
                    int idx = 2;
                    mainActivity.showToast(text, idx);
                    sgl.peakSound = true;
                    sgl.setNowTime();
                    //                mainActivity.setBTSoc(true);

                    byte[] data = new byte[1];
                    data[0] = 0x1b;
                    sgl.sendBTNotice(data);
                    myRecg.boMode_fire_cnt = 0;
                    myRecg.boMode_baby_cnt = 0;
                    myRecg.boMode_bell_cnt = 0;
                    myRecg.boMode_etcc_cnt = 0;
                } else if (myRecg.boMode_baby_cnt == maxVal && myRecg.boMode_etcc_cnt < 5 && sdVal > 600) { // 분산이 안되면서 아기울음이 감지되어야 한다.
                    dropCnt = 0;
                    midCnt = 0;
                    upCnt = 0;
                    final String text = "아기가 울고 있습니다.";
                    int idx = 1;
                    mainActivity.showToast(text, idx);
                    sgl.peakSound = true;
                    sgl.setNowTime();
                    //                        mainActivity.setBTSoc(true);

                    byte[] data = new byte[1];
                    data[0] = 0x1a;
                    sgl.sendBTNotice(data);
                    myRecg.boMode_fire_cnt = 0;
                    myRecg.boMode_baby_cnt = 0;
                    myRecg.boMode_bell_cnt = 0;
                    myRecg.boMode_etcc_cnt = 0;

                } else if (myRecg.boMode_bell_cnt == maxVal && myRecg.boMode_etcc_cnt < 5) { // 분산이 안되면서 초인종이 감지되어야 한다.
                    //                else if(sgl.bellAvgDist < BELLAPPROXIMATE &&
                    //                        sgl.babyAvgDist > APPROXIMATE ) { //평균값으로 비교한다
                    dropCnt = 0;
                    midCnt = 0;
                    upCnt = 0;
                    final String text = "벨이 울리고 있습니다.";
                    int idx = 0;
                    mainActivity.showToast(text, idx);
                    sgl.peakSound = true;
                    sgl.setNowTime();
                    //                    mainActivity.setBTSoc(true);

                    byte[] data = new byte[1];
                    data[0] = 0x1d;
                    sgl.sendBTNotice(data);

                    myRecg.boMode_fire_cnt = 0;
                    myRecg.boMode_baby_cnt = 0;
                    myRecg.boMode_bell_cnt = 0;
                    myRecg.boMode_etcc_cnt = 0;

                }
                else if (myRecg.boMode_etcc_cnt == maxVal && sdVal <= 600) {
                    dropCnt = 0;
                    midCnt = 0;
                    upCnt = 0;
                    final String text = "큰 소리가 나고 있습니다.";
                    int idx = 3;
                    mainActivity.showToast(text, idx);
                    sgl.peakSound = true;
                    sgl.setNowTime();
                    //                        mainActivity.setBTSoc(true);

                    byte[] data = new byte[1];
                    data[0] = 0x1c;
                    sgl.sendBTNotice(data);
                    myRecg.boMode_fire_cnt = 0;
                    myRecg.boMode_baby_cnt = 0;
                    myRecg.boMode_bell_cnt = 0;
                    myRecg.boMode_etcc_cnt = 0;
                }
                else {
                    dropCnt = 0;
                    midCnt = 0;
                    upCnt = 0;
                    myRecg.boMode_fire_cnt = 0;
                    myRecg.boMode_baby_cnt = 0;
                    myRecg.boMode_bell_cnt = 0;
                    myRecg.boMode_etcc_cnt = 0;
                    int idx = 4;
                    final String text = "잠잠합니다.";
                    mainActivity.showToast(text, idx);
                }
            }
            else {
                dropCnt = 0;
                midCnt = 0;
                upCnt = 0;
                myRecg.boMode_fire_cnt = 0;
                myRecg.boMode_baby_cnt = 0;
                myRecg.boMode_bell_cnt = 0;
                myRecg.boMode_etcc_cnt = 0;
                int idx = 4;
                final String text = "잠잠합니다.";
                mainActivity.showToast(text, idx);
            }
            // 마지막 큐의 내용을 꺼내 지운다.
            Frequeue.poll();
        }
        sgl.oldRmsdB = rmsdB;

    }

//    void decideWhatKindOfSound4(double rmsdB, boolean dropCheck) {
//
//        getSpectrumAmpDB();
////        boolean retVal = false;
//        // Find and show peak amplitude
//        maxAmpDB  = 20 * log10(0.125/32768.0);
//        maxAmpFreq = 0;
//        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
//            if (spectrumAmpOutDB[i] > maxAmpDB) {
//                maxAmpDB  = spectrumAmpOutDB[i];
//                maxAmpFreq = i;
//            }
//        }
//
//        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
//            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
//            double x1 = spectrumAmpOutDB[id-1];
//            double x2 = spectrumAmpOutDB[id];
//            double x3 = spectrumAmpOutDB[id+1];
//            double c = x2;
//            double a = (x3+x1)/2 - x2;
//            double b = (x3-x1)/2;
//            if (a < 0) {
//                double xPeak = -b/(2*a);
//                if (abs(xPeak) < 1) {
//                    maxAmpFreq += xPeak * sampleRate / fftLen;
//                    maxAmpDB = (4*a*c - b*b)/(4*a);
//                }
//            }
//        }
//
//        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//        Log.d("TAG", "maxAmpFreq 진동수 = " +  maxAmpFreq + " maxAmpDB = " + maxAmpDB);
////        fireFrequeue.offer(maxAmpFreq);
//        Frequeue.offer(maxAmpFreq);
//
//        //큐를 고정으로 하고 비교 해보자....
//        if(Frequeue.size() >= 50) {
//            // 1. 음악이나, 사이렌은 연속음이기에 dB이 떨어지는 구간이 없다.
//            //    따라서 dB이 떨어지는 구간이 있으면, 그것은 음악이나 사이렌이 아니다.
//            // 2. 진동수는 사이렌도 그렇게 높지 않은 것도 있으므로 진동수로 기준을 잡아서는 안된다.
//            // 3. 일단 실시간으로 아기울음이나 초인종인지를 계속 판단하고 있되, 3초가 되기 전에는 결정을 하지 않는다.
//
//            float bellDist = 999999999;
//            float babyDist = 999999999;
//
//            // 4. 먼저 아기울음 패턴과 비교하여 점수를 매긴다.
//            //루프가 너무 느리다 돌때 들어오는 소리들을 감지하지 못한다.
//            for (ArrayList<Double> arr : readSnd.getSoundPatternArr()) {
//                int minSize = min(Frequeue.size(), arr.size());
//                // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                Point[] pattenPoint = new Point[arr.size()];
//                Point[] targetPoint = new Point[Frequeue.size()];
//                int i = 0;
//                for (double nowFreq : Frequeue) {
//                    if (i >= minSize) {
//                    } else {
//                        pattenPoint[i] = new Point();
//                        targetPoint[i] = new Point();
//                        pattenPoint[i].x = i;
//                        pattenPoint[i].y = arr.get(i).intValue();
//                        targetPoint[i].x = i;
//                        targetPoint[i].y = (int) nowFreq;
//                    }
//                    i++;
//                }
//
//                float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
//                //Log.d("TAG", "아기울음 패턴 최소거리 = " +  dist);
////                    if (dist < APPROXIMATE) {
////
////                        myRecg.boMode_baby_cnt++;
////                        if (myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT) {
////                            //myRecg.boMode_baby_cnt = 0;
////                            babyDist = dist;
////                        }
////                    }
//
//                //평균값을 구하기위해 전체 값을 구한다
//                sgl.babyTotalDist = sgl.babyTotalDist + dist;
//                sgl.babyDistCnt++;
//
//            }
//
//            // 5. 다음 초인종 패턴과 비교하여 점수를 매긴다.
//
//            for (ArrayList<Double> arr : readSnd.getBellSoundPatternArr()) {
//                int minSize = min(Frequeue.size(), arr.size());
//                // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                Point[] pattenPoint = new Point[arr.size()];
//                Point[] targetPoint = new Point[Frequeue.size()];
//                int i = 0;
//                for (double nowFreq : Frequeue) {
//                    if (i >= minSize) {
//                    } else {
//                        pattenPoint[i] = new Point();
//                        targetPoint[i] = new Point();
//                        pattenPoint[i].x = i;
//                        pattenPoint[i].y = arr.get(i).intValue();
//                        targetPoint[i].x = i;
//                        targetPoint[i].y = (int) nowFreq;
//                    }
//                    i++;
//                }
//
//                float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
////                    if (dist < BELLAPPROXIMATE) {
////
////                        myRecg.boMode_bell_cnt++;
////                        if (myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT) {
////                            //myRecg.boMode_bell_cnt = 0;
////                            bellDist = dist;
////                        }
////                    }
//
//                //평균값을 구하기위해 전체 값을 구한다
//                sgl.bellTotalDist = sgl.bellTotalDist +dist;
//                sgl.bellDistCnt++;
//            }
//            //
//            //     6. 전체 3개 유형 비교한다.
//            //     - 화재경보가 최우선
//            //
//            int rmsResult = compare((float)rmsdB, DROP_DECIBEL);
//            if(rmsResult < 0 ) {    // 40dB 이 조용할때 암소음의 dB임(즉 소리가 없을 때)..그러나 현실은 다름..
//                dropCnt++;
//            }
//            int rmsMResult = compare((float)rmsdB, MID_DECIBEL);
//            if(rmsMResult > 0 ) {    // 60dB 을 보통 말소리 데시벨로 가정
//                midCnt++;
//            }
//            int rmsHResult = compare((float)rmsdB, HIGH_DECIBEL);
//            if(rmsHResult > 0 ) {    // 70dB 을 화재경보의 데시벨로 가정
//                upCnt++;
//            }
////            float updownRate = dropCnt/midCnt;
////
//            int cmpResult = compare((float)sgl.oldRmsdB, 0);
//            if (cmpResult != 0) {
//                double grad = sgl.oldRmsdB - rmsdB;
//                Log.d("TAG", "grad? = "+grad);
//            }
//
//            if( sgl.startMonitor == false) {// 최초 감시 시작단계에서는 결정을 내리지 않는다.
//                sgl.startMonitor = true;
//                counterReset();
//                Frequeue.poll();
//                return;
//            }
//            if(dropCheck == false) { // 3초 이내에는 결정을 내리지 않는다.(사이렌이나 음악소리 등 연속음일 수 있음)
////                여기서 리셋하면 절대 안된다. 이부분은 3초내에 수십번 도는 곳이다.
////                myRecg.boMode_fire_cnt = 0;
////                myRecg.boMode_baby_cnt = 0;
////                myRecg.boMode_bell_cnt = 0;
//
//                Frequeue.poll();
//                return;
//            }
//            Log.d("TAG", "3초가 지나 결정시기가 옴.....dropCnt = " + dropCnt + " upCnt = " + upCnt + " midCnt = " + midCnt);
//            sgl.babyAvgDist = sgl.babyTotalDist/sgl.babyDistCnt;
//            sgl.bellAvgDist = sgl.bellTotalDist/sgl.bellDistCnt;
////            sgl.babyAvgDist = sgl.babyTotalDist/myRecg.boMode_baby_cnt;
////            sgl.bellAvgDist = sgl.bellTotalDist/myRecg.boMode_bell_cnt;
//            sgl.babyTotalDist = sgl.bellTotalDist = sgl.babyDistCnt = sgl.bellDistCnt = 0;
//            // 암소음 구간이 3초동안 10회보다 작으MAX_FIREDICISION_CNT면서, 80dB 이상 구간이 10회보다 크면 사이렌이나 음악소리이다.
//            if ( dropCnt < myRecg.MAX_FIREDICISION_CNT  && upCnt > myRecg.MAX_FIREDICISION_CNT)
//            {
//                dropCnt = 0;
//                midCnt = 0;
//                upCnt = 0;
//
//                final String text = "화재경보가 울리고 있습니다.";
//                int idx = 2;
//                mainActivity.showToast(text, idx);
//                sgl.peakSound = true;
//                sgl.setNowTime();
//                mainActivity.setBTSoc(true);
//
//                byte[] data = new byte[1];
//                data[0] = 0x1b;
//                sgl.sendBTNotice(data);
//                myRecg.boMode_fire_cnt = 0;
//                myRecg.boMode_baby_cnt = 0;
//                myRecg.boMode_bell_cnt = 0;
//
//            }
//            else {
//                dropCnt = 0;
//                midCnt = 0;
//                upCnt = 0;
//                //화재가 아니면 바로 초기화가 되는건가? 아니면 계속 카운트 해야되는가..
//
////                    if (myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT &&
////                            myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT ) {
//                if (sgl.bellAvgDist < BELLAPPROXIMATE &&
//                        sgl.babyAvgDist < APPROXIMATE ) { //평균값으로 비교한다
//                    //벨 울음 비교
////                        Log.d("compare", "bellDist: " + bellDist);
////                        Log.d("compare", "babyDist: " + babyDist);
//                    Log.d("compare", "bellAvgDist: " + sgl.bellAvgDist);
//                    Log.d("compare", "babyAvgDist: " + sgl.babyAvgDist);
//
////                        int result = compare(bellDist, babyDist);
//                    int result = compare(sgl.bellAvgDist, sgl.babyAvgDist);
//                    if (result == 0) {
//                        Log.d("compare", "compare Equals");
//                    } else if (result < 0) {
////                        dropCnt = 0;
////                        midCnt = 0;
////                        upCnt = 0;
//
//                        final String text = "벨이 울리고 있습니다.";
//                        int idx = 0;
//                        mainActivity.showToast(text, idx);
//                        sgl.peakSound = true;
//                        sgl.setNowTime();
//                        mainActivity.setBTSoc(true);
//
//                        byte[] data = new byte[1];
//                        data[0] = 0x1d;
//                        sgl.sendBTNotice(data);
//
//                    } else {
////                        dropCnt = 0;
////                        midCnt = 0;
////                        upCnt = 0;
//
//                        final String text = "아기가 울고 있습니다.";
//                        int idx = 1;
//                        mainActivity.showToast(text, idx);
//                        sgl.peakSound = true;
//                        sgl.setNowTime();
////                        mainActivity.setBTSoc(true);
//
//                        byte[] data = new byte[1];
//                        data[0] = 0x1a;
//                        sgl.sendBTNotice(data);
//                    }
//                }
////                    else if(myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT &&
////                            myRecg.boMode_bell_cnt < myRecg.MAX_DICISION_BELL_CNT ) {
//                else if(sgl.bellAvgDist > BELLAPPROXIMATE &&
//                        sgl.babyAvgDist < APPROXIMATE ) { //평균값으로 비교한다
//                    final String text = "아기가 울고 있습니다.";
//                    int idx = 1;
//                    mainActivity.showToast(text, idx);
//                    sgl.peakSound = true;
//                    sgl.setNowTime();
////                    mainActivity.setBTSoc(true);
//
//                    byte[] data = new byte[1];
//                    data[0] = 0x1a;
//                    sgl.sendBTNotice(data);
//                }
////                else if(myRecg.boMode_baby_cnt < myRecg.MAX_DICISION_CNT &&
////                        myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT ) {
//                else if(sgl.bellAvgDist < BELLAPPROXIMATE &&
//                        sgl.babyAvgDist > APPROXIMATE ) { //평균값으로 비교한다
//                    final String text = "벨이 울리고 있습니다.";
//                    int idx = 0;
//                    mainActivity.showToast(text, idx);
//                    sgl.peakSound = true;
//                    sgl.setNowTime();
////                    mainActivity.setBTSoc(true);
//
//                    byte[] data = new byte[1];
//                    data[0] = 0x1d;
//                    sgl.sendBTNotice(data);
//
//                }
//
//                myRecg.boMode_fire_cnt = 0;
//                myRecg.boMode_baby_cnt = 0;
//                myRecg.boMode_bell_cnt = 0;
//            }
//
//            // 마지막 큐의 내용을 꺼내 지운다.
//            Frequeue.poll();
//        }
//        sgl.oldRmsdB = rmsdB;
//
//    }

/////////////////2222222222222
//void decideWhatKindOfSound3(double rmsdB, boolean dropCheck) {
//
//    getSpectrumAmpDB();
////        boolean retVal = false;
//    // Find and show peak amplitude
//    maxAmpDB  = 20 * log10(0.125/32768.0);
//    maxAmpFreq = 0;
//    for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
//        if (spectrumAmpOutDB[i] > maxAmpDB) {
//            maxAmpDB  = spectrumAmpOutDB[i];
//            maxAmpFreq = i;
//        }
//    }
//
//    if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
//        int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
//        double x1 = spectrumAmpOutDB[id-1];
//        double x2 = spectrumAmpOutDB[id];
//        double x3 = spectrumAmpOutDB[id+1];
//        double c = x2;
//        double a = (x3+x1)/2 - x2;
//        double b = (x3-x1)/2;
//        if (a < 0) {
//            double xPeak = -b/(2*a);
//            if (abs(xPeak) < 1) {
//                maxAmpFreq += xPeak * sampleRate / fftLen;
//                maxAmpDB = (4*a*c - b*b)/(4*a);
//            }
//        }
//    }
//
//    maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//    Log.d("TAG", "maxAmpFreq 진동수 = " +  maxAmpFreq + " maxAmpDB = " + maxAmpDB);
////        fireFrequeue.offer(maxAmpFreq);
//    Frequeue.offer(maxAmpFreq);
//
//    //큐를 고정으로 하고 비교 해보자....
//    if(Frequeue.size() >= 50) {
//        // 1. 음악이나, 사이렌은 연속음이기에 dB이 떨어지는 구간이 없다.
//        //    따라서 dB이 떨어지는 구간이 있으면, 그것은 음악이나 사이렌이 아니다.
//        // 2. 진동수는 사이렌도 그렇게 높지 않은 것도 있으므로 진동수로 기준을 잡아서는 안된다.
//        // 3. 일단 실시간으로 아기울음이나 초인종인지를 계속 판단하고 있되, 3초가 되기 전에는 결정을 하지 않는다.
//
//        float bellDist = 999999999;
//        float babyDist = 999999999;
//
//        if(dropCheck == true && sgl.startMonitor == true) { // 3초후 한번만.. 계속 체크하면 정말 느려진다.. 감지못하는 수준
//
//            // 4. 먼저 아기울음 패턴과 비교하여 점수를 매긴다.
//            for (ArrayList<Double> arr : readSnd.getSoundPatternArr()) {
//                int minSize = min(Frequeue.size(), arr.size());
//                // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                Point[] pattenPoint = new Point[arr.size()];
//                Point[] targetPoint = new Point[Frequeue.size()];
//                int i = 0;
//                for (double nowFreq : Frequeue) {
//                    if (i >= minSize) {
//                    } else {
//                        pattenPoint[i] = new Point();
//                        targetPoint[i] = new Point();
//                        pattenPoint[i].x = i;
//                        pattenPoint[i].y = arr.get(i).intValue();
//                        targetPoint[i].x = i;
//                        targetPoint[i].y = (int) nowFreq;
//                    }
//                    i++;
//                }
//
//                float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
//                Log.d("TAG", "아기울음 패턴 최소거리 = " +  dist);
//                if (dist < APPROXIMATE) {
//
//                    myRecg.boMode_baby_cnt++;
//                    //if (myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT) {
//                        //myRecg.boMode_baby_cnt = 0;
//                        babyDist = dist;
//                    //}
//                }
//
//            }
//
//            // 5. 다음 초인종 패턴과 비교하여 점수를 매긴다.
//            for (ArrayList<Double> arr : readSnd.getBellSoundPatternArr()) {
//                int minSize = min(Frequeue.size(), arr.size());
//                // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                Point[] pattenPoint = new Point[arr.size()];
//                Point[] targetPoint = new Point[Frequeue.size()];
//                int i = 0;
//                for (double nowFreq : Frequeue) {
//                    if (i >= minSize) {
//                    } else {
//                        pattenPoint[i] = new Point();
//                        targetPoint[i] = new Point();
//                        pattenPoint[i].x = i;
//                        pattenPoint[i].y = arr.get(i).intValue();
//                        targetPoint[i].x = i;
//                        targetPoint[i].y = (int) nowFreq;
//                    }
//                    i++;
//                }
//
//                float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
//                if (dist < BELLAPPROXIMATE) {
//
//                    myRecg.boMode_bell_cnt++;
//                    //if (myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT) {
//                        //myRecg.boMode_bell_cnt = 0;
//                        bellDist = dist;
//                    //}
//                }
//
//            }
//
//        }
//
//        //기울기..
//        //3초 동안 계속 0 근처 값이 나오면 기울기가 0 -> 수평 dB
//        double grad = 0;
//        boolean grad0 = false;
//        int cmpResult = compare((float)sgl.oldRmsdB, 0);
//        if (cmpResult != 0) {
//            grad = sgl.oldRmsdB - rmsdB;
//            Log.d("TAG", "grad? = "+ grad);
//            if (Math.abs(grad) > 1) {
//                grad0 = false;
//            }else grad0 = true;
//        }
//
//        //
//        //     6. 전체 3개 유형 비교한다.
//        //     - 화재경보가 최우선
//        //
//        int rmsResult = compare((float)rmsdB, DROP_DECIBEL); //에어컨 돌아가는 소리가 40후반에서 50초반으로 나타남 s4
//        if(rmsResult < 0 ) {    // 40dB 이 조용할때 암소음의 dB임(즉 소리가 없을 때)..그러나 현실은 다름..
//            dropCnt++;
//        }
//        int rmsMResult = compare((float)rmsdB, MID_DECIBEL);
//        if(rmsMResult > 0 ) {    // 60dB 을 보통 말소리 데시벨로 가정
//            midCnt++;
//        }
//        int rmsHResult = compare((float)rmsdB, HIGH_DECIBEL); // 기준이 너무 낮으면 아기울음도 카운트가 된다.. 높으면 화재경보 카운트가 안됨.. 화재경보 55dB도 안되는 경우가 많다
//        if(rmsHResult > 0 && grad0 == true) {    // 70dB 을 화재경보의 데시벨로 가정
//            upCnt++;
//        }
//        Log.d("TAG", "dropCnt = " + dropCnt + " .. upCnt = " + upCnt + " .. midCnt = " + midCnt);
//
////            float updownRate = dropCnt/midCnt;
////
//
//        if( sgl.startMonitor == false) {// 최초 감시 시작단계에서는 결정을 내리지 않는다.
//            sgl.startMonitor = true;
//            counterReset();
//            Frequeue.poll();
//            return;
//        }
//
//        if(dropCheck == false) { // 3초 이내에는 결정을 내리지 않는다.(사이렌이나 음악소리 등 연속음일 수 있음)
////                여기서 리셋하면 절대 안된다. 이부분은 3초내에 수십번 도는 곳이다.
////                myRecg.boMode_fire_cnt = 0;
////                myRecg.boMode_baby_cnt = 0;
////                myRecg.boMode_bell_cnt = 0;
//
//            Frequeue.poll();
//            return;
//        }
//        Log.d("TAG", "3초가 지나 결정시기가 옴.....dropCnt = " + dropCnt + " upCnt = " + upCnt + " midCnt = " + midCnt);
//        // 암소음 구간이 3초동안 10회보다 작으MAX_FIREDICISION_CNT면서, 80dB 이상 구간이 10회보다 크면 사이렌이나 음악소리이다.
//        if ( dropCnt < myRecg.MAX_FIREDICISION_CNT  && upCnt > myRecg.MAX_FIREDICISION_CNT)
//        {
//            dropCnt = 0;
//            midCnt = 0;
//            upCnt = 0;
//
//            final String text = "화재경보가 울리고 있습니다.";
//            int idx = 2;
//            mainActivity.showToast(text, idx);
//            sgl.peakSound = true;
//            sgl.setNowTime();
////            mainActivity.setBTSoc(true);
//
//            byte[] data = new byte[1];
//            data[0] = 0x1b;
//            sgl.sendBTNotice(data);
//            myRecg.boMode_fire_cnt = 0;
//            myRecg.boMode_baby_cnt = 0;
//            myRecg.boMode_bell_cnt = 0;
//
//        }
//        else {
//            dropCnt = 0;
//            midCnt = 0;
//            upCnt = 0;
//            //화재가 아니면 바로 초기화가 되는건가? 아니면 계속 카운트 해야되는가..
//            // 벨/아기울음 판단하는곳에서 초기화하면 그전까지 무한으로 카운트가 늘어남
//            //3초안에 판단하지 못하면 감지 못함.. 벨/아기울음은 1번에 감지 해야함
//
//            if (myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT &&
//                    myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT ) {
//                //벨 울음 비교
//                Log.d("compare", "bellDist: " + bellDist);
//                Log.d("compare", "babyDist: " + babyDist);
//
//                int result = compare(bellDist, babyDist);
//                if (result == 0) {
//                    Log.d("compare", "compare Equals");
//                } else if (result < 0) {
//                    final String text = "벨이 울리고 있습니다.";
//                    int idx = 0;
//                    mainActivity.showToast(text, idx);
//                    sgl.peakSound = true;
//                    sgl.setNowTime();
//                    mainActivity.setBTSoc(true);
//
//                    byte[] data = new byte[1];
//                    data[0] = 0x1d;
//                    sgl.sendBTNotice(data);
//
//                } else {
//                    final String text = "아기가 울고 있습니다.";
//                    int idx = 1;
//                    mainActivity.showToast(text, idx);
//                    sgl.peakSound = true;
//                    sgl.setNowTime();
//                    mainActivity.setBTSoc(true);
//
//                    byte[] data = new byte[1];
//                    data[0] = 0x1a;
//                    sgl.sendBTNotice(data);
//                }
//            }
//            else if(myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT &&
//                    myRecg.boMode_bell_cnt < myRecg.MAX_DICISION_BELL_CNT ) {
//                final String text = "아기가 울고 있습니다.";
//                int idx = 1;
//                mainActivity.showToast(text, idx);
//                sgl.peakSound = true;
//                sgl.setNowTime();
//                mainActivity.setBTSoc(true);
//
//                byte[] data = new byte[1];
//                data[0] = 0x1a;
//                sgl.sendBTNotice(data);
//            }
//            else if(myRecg.boMode_baby_cnt < myRecg.MAX_DICISION_CNT &&
//                    myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT ) {
//                final String text = "벨이 울리고 있습니다.";
//                int idx = 0;
//                mainActivity.showToast(text, idx);
//                sgl.peakSound = true;
//                sgl.setNowTime();
//                mainActivity.setBTSoc(true);
//
//                byte[] data = new byte[1];
//                data[0] = 0x1d;
//                sgl.sendBTNotice(data);
//
//            }
//
//            myRecg.boMode_fire_cnt = 0;
//            myRecg.boMode_baby_cnt = 0;
//            myRecg.boMode_bell_cnt = 0;
//        }
//
//        // 마지막 큐의 내용을 꺼내 지운다.
//        Frequeue.poll();
//    }
//    sgl.oldRmsdB = rmsdB;
//
//}

//    void calculatePeakkk(double rmsdB) {
//        getSpectrumAmpDB();
////        boolean retVal = false;
//        // Find and show peak amplitude
//        maxAmpDB  = 20 * log10(0.125/32768);
//        maxAmpFreq = 0;
//        for (int i = 1; i < spectrumAmpOutDB.length; i++) {  // skip the direct current term
//            if (spectrumAmpOutDB[i] > maxAmpDB) {
//                maxAmpDB  = spectrumAmpOutDB[i];
//                maxAmpFreq = i;
//            }
//        }
//
//        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate/2 - sampleRate / fftLen) {
//            int id = (int)(round(maxAmpFreq/sampleRate*fftLen));
//            double x1 = spectrumAmpOutDB[id-1];
//            double x2 = spectrumAmpOutDB[id];
//            double x3 = spectrumAmpOutDB[id+1];
//            double c = x2;
//            double a = (x3+x1)/2 - x2;
//            double b = (x3-x1)/2;
//            if (a < 0) {
//                double xPeak = -b/(2*a);
//                if (abs(xPeak) < 1) {
//                    maxAmpFreq += xPeak * sampleRate / fftLen;
//                    maxAmpDB = (4*a*c - b*b)/(4*a);
//                }
//            }
//        }
//
//        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;
//        Log.d("TAG", "maxAmpFreq 진동수 = " +  maxAmpFreq);
////        fireFrequeue.offer(maxAmpFreq);
//        Frequeue.offer(maxAmpFreq);
//
//        long timeNow = SystemClock.uptimeMillis();
//
//        //큐를 고정으로 하고 비교 해보자....
//        if(Frequeue.size() >= 50) {
//
////            if (rmsdB > 45)
//            {
////                double total = 0;
//                double preFreq = 0.0F;
//                for (double nowFreq : Frequeue) {
//                    if(nowFreq < 100) {
//                        dropCnt++;
//                    }
//                    else if(nowFreq > 2000) {
//                        upCnt++;
//                        if(abs(preFreq - nowFreq) < 40) {  // 화재경보 팩터
//                            similarCnt++;
//                        }
//                    }
//                    preFreq = nowFreq;
//                }
//
//                if(similarCnt > 15) {       // 인접한 수치간의 비슷한 갯수가 30개 이상일때 사이렌으로 인식
//                    if ((double) upCnt / dropCnt > 2.0F) { // 사이렌은 진동수가 심각하게 떨어져서는 안된다. 3번이상..
//                        myRecg.boMode_fire_cnt++;
//                    } else if ((double) upCnt / dropCnt < 0.1F) { // 사이렌이 잠잠해질때 리셋함..
//                        myRecg.boMode_fire_cnt = 0;
//                    } else if (dropCnt == 0 && upCnt == 0) {
//                        myRecg.boMode_fire_cnt = 0;
//                    }
//                }
//                Log.d("tag", " DropCnt :" + dropCnt + " UpCnt : " + upCnt + "fire_cnt : " + myRecg.boMode_fire_cnt);
//                dropCnt = upCnt = similarCnt = 0;
//            }
//
//            // 마지막 큐의 내용을 꺼내 지운다.
////            fireFrequeue.poll();      // 지금 지우지 말고 다 판단하고난 후 지운다.
//
//
////            if (rmsdB > 45)
//            {
//
//                float bellDist = 999999999;
//                float babyDist = 999999999;
//
//                for (ArrayList<Double> arr : readSnd.getSoundPatternArr()) {
//                    int minSize = min(Frequeue.size(), arr.size());
//                    // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                    Point[] pattenPoint = new Point[arr.size()];
//                    Point[] targetPoint = new Point[Frequeue.size()];
//                    int i = 0;
//                    for (double nowFreq : Frequeue) {
//                        if (i >= minSize) {
//                        } else {
//                            pattenPoint[i] = new Point();
//                            targetPoint[i] = new Point();
//                            pattenPoint[i].x = i;
//                            pattenPoint[i].y = arr.get(i).intValue();
//                            targetPoint[i].x = i;
//                            targetPoint[i].y = (int) nowFreq;
//                        }
//                        i++;
//                    }
//
//                    float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
//                    if (dist < APPROXIMATE) {
//
//                        myRecg.boMode_baby_cnt++;
//                        if (myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT) {
//                            //myRecg.boMode_baby_cnt = 0;
//                            babyDist = dist;
//                        }
//
//                    }
//                }
//
//                for (ArrayList<Double> arr : readSnd.getBellSoundPatternArr()) {
//                    int minSize = min(Frequeue.size(), arr.size());
//                    // 이방법이 적중도가 높다. 그러나 기타 소리를 판별하는데 문제가 있다.
//                    Point[] pattenPoint = new Point[arr.size()];
//                    Point[] targetPoint = new Point[Frequeue.size()];
//                    int i = 0;
//                    for (double nowFreq : Frequeue) {
//                        if (i >= minSize) {
//                        } else {
//                            pattenPoint[i] = new Point();
//                            targetPoint[i] = new Point();
//                            pattenPoint[i].x = i;
//                            pattenPoint[i].y = arr.get(i).intValue();
//                            targetPoint[i].x = i;
//                            targetPoint[i].y = (int) nowFreq;
//                        }
//                        i++;
//                    }
//
//                    float dist = jjSoundCloudMatch(targetPoint, pattenPoint);
//                    if (dist < BELLAPPROXIMATE) {
//
//                        myRecg.boMode_bell_cnt++;
//                        if (myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT) {
//                            //myRecg.boMode_bell_cnt = 0;
//                            bellDist = dist;
//                        }
//
//                    }
//                }
////
////     전체 3개 유형 비교()
////     - 화재경보가 최우선
////
//                // 높은 진동수 소리의 변화가 없는 상태가 8번 이상 지속된 경우
//                if (myRecg.boMode_fire_cnt >= myRecg.MAX_FIREDICISION_CNT) {
//                    final String text = "화재경보가 울리고 있습니다.";
//                    int idx = 2;
//                    mainActivity.showToast(text, idx);
//                    sgl.peakSound = true;
//                    sgl.setNowTime();
//                    mainActivity.setBTSoc(true);
//
//                    byte[] data = new byte[1];
//                    data[0] = 0x1b;
//                    sgl.sendBTNotice(data);
//                    myRecg.boMode_fire_cnt = 0;
//                    myRecg.boMode_baby_cnt = 0;
//                    myRecg.boMode_bell_cnt = 0;
//
//                } else {
//
//                    if (myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT &&
//                        myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT ) {
//                        //벨 울음 비교
//                        Log.d("compare", "bellDist: " + bellDist);
//                        Log.d("compare", "babyDist: " + babyDist);
//
//                        int result = compare(bellDist, babyDist);
//                        if (result == 0) {
//                            Log.d("compare", "compare Equals");
//                        } else if (result < 0) {
//                            final String text = "벨이 울리고 있습니다.";
//                            int idx = 0;
//                            mainActivity.showToast(text, idx);
//                            sgl.peakSound = true;
//                            sgl.setNowTime();
//                            mainActivity.setBTSoc(true);
//
//                            byte[] data = new byte[1];
//                            data[0] = 0x1d;
//                            sgl.sendBTNotice(data);
//
////                            myRecg.boMode_fire_cnt = 0;
//                            myRecg.boMode_baby_cnt = 0;
//                            myRecg.boMode_bell_cnt = 0;
//                        } else {
//                            final String text = "아기가 울고 있습니다.";
//                            int idx = 1;
//                            mainActivity.showToast(text, idx);
//                            sgl.peakSound = true;
//                            sgl.setNowTime();
//                            mainActivity.setBTSoc(true);
//
//                            byte[] data = new byte[1];
//                            data[0] = 0x1a;
//                            sgl.sendBTNotice(data);
//
////                            myRecg.boMode_fire_cnt = 0;
//                            myRecg.boMode_baby_cnt = 0;
//                            myRecg.boMode_bell_cnt = 0;
//                        }
//                    }
//                    else if(myRecg.boMode_baby_cnt > myRecg.MAX_DICISION_CNT &&
//                            myRecg.boMode_bell_cnt < myRecg.MAX_DICISION_BELL_CNT ) {
//                        final String text = "아기가 울고 있습니다.";
//                        int idx = 1;
//                        mainActivity.showToast(text, idx);
//                        sgl.peakSound = true;
//                        sgl.setNowTime();
//                        mainActivity.setBTSoc(true);
//
//                        byte[] data = new byte[1];
//                        data[0] = 0x1a;
//                        sgl.sendBTNotice(data);
////                        myRecg.boMode_fire_cnt = 0;
//                        myRecg.boMode_baby_cnt = 0;
//                        myRecg.boMode_bell_cnt = 0;
//                    }
//                    else if(myRecg.boMode_baby_cnt < myRecg.MAX_DICISION_CNT &&
//                            myRecg.boMode_bell_cnt > myRecg.MAX_DICISION_BELL_CNT ) {
//                        final String text = "벨이 울리고 있습니다.";
//                        int idx = 0;
//                        mainActivity.showToast(text, idx);
//                        sgl.peakSound = true;
//                        sgl.setNowTime();
//                        mainActivity.setBTSoc(true);
//
//                        byte[] data = new byte[1];
//                        data[0] = 0x1d;
//                        sgl.sendBTNotice(data);
////                        myRecg.boMode_fire_cnt = 0;
//                        myRecg.boMode_baby_cnt = 0;
//                        myRecg.boMode_bell_cnt = 0;
//                    }
//                    else {
////                        myRecg.boMode_fire_cnt = 0;
////                        myRecg.boMode_baby_cnt = 0;
////                        myRecg.boMode_bell_cnt = 0;
////                        dropCnt = 0;
//                    }
//
//                }
//
////                    myRecg.boMode_fire_cnt = 0;
////                    myRecg.boMode_baby_cnt = 0;
////                    myRecg.boMode_bell_cnt = 0;
////                    dropCnt = 0;
//
//            }
//
//            // 마지막 큐의 내용을 꺼내 지운다.
//            Frequeue.poll();
//        }
//
//    }

    public static float SqrRootPitagorasDist(Point a, Point b)
    {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
    }

//    private static float jjSoundCloudMatch(Point[] points1, Point[] points2)
//    {
//        int n = points1.length;
//        float eps = 0.5f;
//        int step = (int)Math.floor(Math.pow(n, 1.0f - eps));
//        float minDistance = Float.MAX_VALUE;
//        for (int i = 0; i < n; i += step) {
//            float dist1 = jjCloudDistance(points1, points2, i);
//            float dist2 = jjCloudDistance(points2, points1, i);
//
//            minDistance = Math.min(minDistance, Math.min(dist1, dist2));
//        }
//        return minDistance;
//    }

    private static float jjSoundCloudMatch(Point[] points1, Point[] points2)
{
    int n = points1.length;
    int m = points2.length;
    int minIdx = min(n,m);

    float eps = 0.5f;
    int step = (int)Math.floor(Math.pow(minIdx, 1.0f - eps));
    float minDistance = Float.MAX_VALUE;
    for (int i = 0; i < minIdx; i += step) {
        float dist1 = jjCloudDistance(points1, points2, i);
        float dist2 = jjCloudDistance(points2, points1, i);

        minDistance = Math.min(minDistance, Math.min(dist1, dist2));
    }
    return minDistance;
}

//    private static float jjCloudDistance(Point[] points1, Point[] points2, int startIndex)
//    {
//        int n = points1.length;
//        boolean[] matched = new boolean[n];
//        float sum = 0;
//        int i = startIndex;
//        do {
//            int index = -1;
//            float minDistance = Float.MAX_VALUE;
//            for(int j = 0; j < n; j++)
//                if (!matched[j])
//                {
//                    float dist = SqrRootPitagorasDist(points1[i], points2[j]);
//                    if (dist < minDistance)
//                    {
//                        minDistance = dist;
//                        index = j;
//                    }
//                }
//            matched[index] = true;
//            float weight = 1.0f - ((i - startIndex + n) % n) / (1.0f * n);
//            sum += weight * minDistance;
//            i = (i + 1) % n;
//        } while (i != startIndex);
//        return sum;
//    }

    private static float jjCloudDistance(Point[] points1, Point[] points2, int startIndex)
    {
        int n = points1.length;
        int m = points2.length;
        int minIdx = min(n,m);
        boolean[] matched = new boolean[minIdx];
        float sum = 0;
        int i = startIndex;
        do {
            int index = -1;
            float minDistance = Float.MAX_VALUE;
            for(int j = 0; j < minIdx; j++)
                if (!matched[j])
                {
                    //Log.d("Tag", "points1.length: " + points1.length + " i: " + i + " j: " + j);
                    float dist = SqrRootPitagorasDist(points1[i], points2[j]);
                    if (dist < minDistance)
                    {
                        minDistance = dist;
                        index = j;
                    }
                }
            matched[index] = true;
            float weight = 1.0f - ((i - startIndex + minIdx) % minIdx) / (1.0f * minIdx);
            sum += weight * minDistance;
            i = (i + 1) % minIdx;
        } while (i != startIndex);
        return sum;
    }


    void clear() {
        spectrumAmpPt = 0;
        Arrays.fill(spectrumAmpOut, 0.0);
        Arrays.fill(spectrumAmpOutDB, log10(0));
        Arrays.fill(spectrumAmpOutCum, 0.0);
//        for (int i = 0; i < spectrumAmpOutArray.length; i++) {
//            Arrays.fill(spectrumAmpOutArray[i], 0.0);
//        }
    }

    private class calcuPeakThread extends Thread {
        private static final String TAG = "calcuPeakThread";
        public void run() {

        }
    }

}
