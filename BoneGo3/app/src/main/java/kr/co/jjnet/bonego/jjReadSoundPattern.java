package kr.co.jjnet.bonego;

/**
 * Copyright (C) 2017 JJNET Co., Ltd
 * 모든 권리 보유.
 * Developed by JJNET Co., Ltd.
 **/

import android.media.AudioManager;

import java.util.ArrayList;

public class jjReadSoundPattern {

    public AudioManager audioManager;
    private int MAX_PATTERN = 55;
    private int MAX_TOLERANCE = 50;

    public boolean startReadSound = false;

    //private static double[] freqPatternData;
    private static ArrayList<Double> freqPatternData = new ArrayList<Double>();

    //private static ArrayList<double[]> freqPatternDataArr = new ArrayList<double[]>();
    private static ArrayList<ArrayList<Double>> freqPatternDataArr = new ArrayList<>();
    //private static ArrayList<ArrayList<Double>> freqPatternDataArr = new ArrayList<ArrayList<Double>>();

    private static ArrayList<Double> exFreqPatternData = new ArrayList<Double>();
    private static ArrayList<ArrayList<Double>> exFreqPatternDataArr = new ArrayList<>();

    private static ArrayList<Double> bellFreqPatternData = new ArrayList<Double>();
    private static ArrayList<ArrayList<Double>> bellFreqPatternDataArr = new ArrayList<>();

    private static ArrayList<Double> fireFreqPatternData = new ArrayList<Double>();
    private static ArrayList<ArrayList<Double>> fireFreqPatternDataArr = new ArrayList<>();

    private jjReadSoundPattern () {

        //freqPatternData = new double[MAX_PATTERN];
    }

    private static class Singleton {
        private static final jjReadSoundPattern instance = new jjReadSoundPattern();
    }

    public static jjReadSoundPattern getInstance () {
        System.out.println("create instance");
        return Singleton.instance;
    }

    public static void addSoundPattern(String freqStr, int i) {
        //freqPatternData[i] =  Double.parseDouble(freqStr);
        freqPatternData.add(Double.parseDouble(freqStr));
    }

    public static void initialSoundPattern() {
        //freqPatternData = new double[maxPattern];
        freqPatternData = new ArrayList<Double>();
    }

    public static void addSoundPatternArr() {
        freqPatternDataArr.add(freqPatternData);
    }

//    public double[] getSoundPattern() {
//        return freqPatternData;
//    }
    public ArrayList<Double> getSoundPattern() {
        return freqPatternData;
    }

    public ArrayList<Double> getSoundPattern(int idx) {
        return freqPatternDataArr.get(idx);
    }

    public ArrayList<ArrayList<Double>> getSoundPatternArr() {
        return freqPatternDataArr;
    }

    public int getPatternMaxCnt() {
        return MAX_PATTERN;
    }

    public int getPatternMaxCnt(int idx) {
        return freqPatternDataArr.get(idx).size();
    }

    ////////////////////////////////////////////////////////////////////////////
    //예외 패턴
    ////////////////////////////////////////////////////////////////////////////
    public static void addExSoundPattern(String freqStr, int i) {
        exFreqPatternData.add(Double.parseDouble(freqStr));
    }

    public static void initialExSoundPattern() {
        exFreqPatternData = new ArrayList<Double>();
    }

    public static void addExSoundPatternArr() {
        exFreqPatternDataArr.add(exFreqPatternData);
    }

    public ArrayList<Double> getExSoundPattern() {
        return exFreqPatternData;
    }

    public ArrayList<Double> getExSoundPattern(int idx) {
        return exFreqPatternDataArr.get(idx);
    }

    public ArrayList<ArrayList<Double>> getExSoundPatternArr() {
        return exFreqPatternDataArr;
    }

    public int getExPatternMaxCnt(int idx) {
        return exFreqPatternDataArr.get(idx).size();
    }
    /////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    //벨소리 패턴
    ////////////////////////////////////////////////////////////////////////////
    public static void addBellSoundPattern(String freqStr, int i) {
        bellFreqPatternData.add(Double.parseDouble(freqStr));
    }

    public static void initialBellSoundPattern() {
        bellFreqPatternData = new ArrayList<Double>();
    }

    public static void addBellSoundPatternArr() {
        bellFreqPatternDataArr.add(bellFreqPatternData);
    }

    public ArrayList<Double> getBellSoundPattern() {
        return bellFreqPatternData;
    }

    public ArrayList<Double> getBellSoundPattern(int idx) {
        return bellFreqPatternDataArr.get(idx);
    }

    public ArrayList<ArrayList<Double>> getBellSoundPatternArr() {
        return bellFreqPatternDataArr;
    }

    public int getBellPatternMaxCnt(int idx) {
        return bellFreqPatternDataArr.get(idx).size();
    }

    ////////////////////////////////////////////////////////////////////////////
    //사이렌 패턴
    ////////////////////////////////////////////////////////////////////////////
    public static void addFireSoundPattern(String freqStr, int i) {
        fireFreqPatternData.add(Double.parseDouble(freqStr));
    }

    public static void initialFireSoundPattern() {
        fireFreqPatternData = new ArrayList<Double>();
    }

    public static void addFireSoundPatternArr() {
        fireFreqPatternDataArr.add(fireFreqPatternData);
    }

    public ArrayList<Double> getFireSoundPattern() {
        return fireFreqPatternData;
    }

    public ArrayList<Double> getFireSoundPattern(int idx) {
        return fireFreqPatternDataArr.get(idx);
    }

    public ArrayList<ArrayList<Double>> getFireSoundPatternArr() {
        return fireFreqPatternDataArr;
    }

    public int getFirePatternMaxCnt(int idx) {
        return fireFreqPatternDataArr.get(idx).size();
    }
    /////////////////////////////////////////////////////////////////////////////

    public int getPatternTolerance() {
        return MAX_TOLERANCE;
    }
}
