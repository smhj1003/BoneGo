//package kr.co.jjnet.jjsmarthelmet;
//
//import android.content.ContentValues;
//import android.content.Context;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.database.sqlite.SQLiteOpenHelper;
//
//import java.util.ArrayList;
//
//import kr.co.jjnet.jjsmarthelmet.model.jjAlert;
//import kr.co.jjnet.jjsmarthelmet.model.jjHistory;
//
///**
// * Copyright (C) 2017 JJNET Co., Ltd
// * 모든 권리 보유.
// * Developed by JJNET Co., Ltd.
// **/
//
//public class jjDBHelper extends SQLiteOpenHelper {
//
//    private static jjDBHelper mInstance = null;
//    public static final String DBNAME = "angelband.db";
//
//    private Context mContext;
//    private SQLiteDatabase mDatabase;
//
//    // jjDBHelper 생성자로 관리할 DB 이름과 버전 정보를 받음
////    public jjDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
////        super(context, name, factory, version);
////    }
//    public jjDBHelper(Context context) {
//        super(context, DBNAME, null, 1);
//        this.mContext = context;
//    }
//
//    // DB를 새로 생성할 때 호출되는 함수
//    @Override
//
//
//    public void onCreate(SQLiteDatabase db) {
//
//    }
//
//    // DB 업그레이드를 위해 버전이 변경될 때 호출되는 함수
//    @Override
//    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//
//    }
//
//    public void openDatabase() {
//        String dbPath = mContext.getDatabasePath(DBNAME).getPath();
//
//        if (mDatabase != null && mDatabase.isOpen()) {
//            return;
//        }
//
//        mDatabase = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
//    }
//
//    public void closeDatabase() {
//        if (mDatabase != null && mDatabase.isOpen()) {
//            mDatabase.close();
//            mDatabase = null;	// 이걸 해줘야 이중 close 로 인한 leak 안생김
//        }
//    }
//
//    /////////////////////////////////////////////////////////////////////////////////////////////////////////
//    //sql
//    /////////////////////////////////////////////////////////////////////////////////////////////////////////
//    public void exeuteSQL(String sqlStr) {
//        SQLiteDatabase db = getWritableDatabase();
//        db.execSQL(sqlStr);
//        db.close();
//    }
//
//    public String getResult(String sqlStr) {
//        // 읽기가 가능하게 DB 열기
//        SQLiteDatabase db = getReadableDatabase();
//        String result = "";
//        Cursor cursor = db.rawQuery(sqlStr, null);
//
//        while (cursor.moveToNext()) {
//            result += cursor.getString(0)
//                    + "\n";
//        }
//
//        return result;
//    }
//
//    /////////////////////////////////////////////////////////////////////////////////////////////////////////
//    //history_sql
//    /////////////////////////////////////////////////////////////////////////////////////////////////////////
//    public long addHistory(jjHistory history) {
//        ContentValues contentValues = new ContentValues();
//        //contentValues.put("idx", (byte[]) null);
//        contentValues.put("bandName", history.getBandName());
//        contentValues.put("bandID", history.getBandID());
//        contentValues.put("alertType", history.getAlertType());
//        contentValues.put("alertContents", history.getAlertContents());
//        contentValues.put("alertLocation", history.getAlertLocation());
//        contentValues.put("micSensitivity", history.getMicSensitivity());
//        contentValues.put("noiseSize", history.getNoiseSize());
//        //contentValues.put("date", (byte[]) null);
//        contentValues.put("extra1", history.getAlertExtra1());
//        contentValues.put("extra2", history.getAlertExtra2());
//        contentValues.put("extra3", history.getAlertExtra3());
//
//        long returnValue = mDatabase.insert("jjHistory", null, contentValues);
//
//        return returnValue;
//    }
//
//    public ArrayList<jjHistory> getListHistory() {
//        jjHistory history = null;
//        String sql = null;
//        ArrayList<jjHistory> historyList = new ArrayList<>();
//
//        sql = "SELECT * FROM jjHistory order by idx desc";
//
//        Cursor cursor = mDatabase.rawQuery(sql, null);
//
//        while (cursor.moveToNext()) {
//            history = new jjHistory(cursor.getString(1)
//                    , cursor.getString(2)
//                    , cursor.getString(3)
//                    , cursor.getString(4)
//                    , cursor.getString(5)
//                    , cursor.getString(6)
//                    , cursor.getString(7)
//                    , cursor.getString(8)
//                    , cursor.getString(9)
//                    , cursor.getString(10)
//                    , cursor.getString(11));
//            historyList.add(history);
//        }
//        cursor.close();
//
//        return historyList;
//    }
//
//    /////////////////////////////////////////////////////////////////////////////////////////////////////////
//    //alert_sql
//    /////////////////////////////////////////////////////////////////////////////////////////////////////////
//    public long addAlert(jjAlert alert) {
//        ContentValues contentValues = new ContentValues();
//        contentValues.put("alert", alert.getAlert());
//        contentValues.put("alertContents", alert.getAlertContents());
//        contentValues.put("alertType", alert.getAlertType());
//        contentValues.put("alertImgPath", alert.getAlertImgPath());
//        contentValues.put("extra1", alert.getAlertExtra1());
//        contentValues.put("extra2", alert.getAlertExtra2());
//        contentValues.put("extra3", alert.getAlertExtra3());
//
//        long returnValue = mDatabase.insert("jjAlert", null, contentValues);
//
//        return returnValue;
//    }
//
//    public ArrayList<jjAlert> getListAlert() {
//        jjAlert alert = null;
//        String sql = null;
//        ArrayList<jjAlert> alertList = new ArrayList<>();
//
//        sql = "SELECT * FROM jjAlert order by idx asc";
//
//        Cursor cursor = mDatabase.rawQuery(sql, null);
//
//        while (cursor.moveToNext()) {
//            alert = new jjAlert(cursor.getString(0)
//                    , cursor.getString(1)
//                    , cursor.getString(2)
//                    , cursor.getString(3)
//                    , cursor.getString(4)
//                    , cursor.getString(5)
//                    , cursor.getString(6));
//            alertList.add(alert);
//        }
//        cursor.close();
//
//        return alertList;
//    }
//
//}
