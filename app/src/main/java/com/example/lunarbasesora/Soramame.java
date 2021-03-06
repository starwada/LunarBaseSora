package com.example.lunarbasesora;

import android.graphics.Color;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * そらまめデータ
 * Intentで渡せるようにParcelableを使用
 * Created by Wada on 2015/07/03.
 * Map無し版（MyApplication）と合わせるように修正。
 * こちら特有のコードは先方に入れてないが。2015/12/17
 */
public class Soramame implements Parcelable {
    // そらまめの測定局データ
    public  class SoramameStation
    {
        private int m_nCode;                // 測定局コード
        private String m_strName;       // 測定局名称
        private String m_strAddress;    // 住所
        private boolean m_bAllow[] = {false, false, false};     // 取得データフラグ（OX、PM2.5、風向）

        public SoramameStation(int nCode, String strName, String strAddress)
        {
            setCode(nCode);
            setName(strName);
            setAddress(strAddress);
        }

        // Set
        public void setCode(int nCode)
        {
            m_nCode = nCode;
        }
        public void setName(String strName)
        {
            m_strName = strName;
        }
        public void setAddress(String strAddress)
        {
            m_strAddress = strAddress;
        }
        public void setAllow(boolean bAllow[]) {
            m_bAllow = bAllow;
        }

        // Get
        public int getCode()
        {
            return m_nCode;
        }
        public String getName()
        {
            return m_strName;
        }
        public String getAddress()
        {
            return m_strAddress;
        }
        public String getString()
        {
            return String.format("%d %s:%s", m_nCode, m_strName, m_strAddress);
        }
        public boolean[] getAllow() {
            return m_bAllow;
        }

        public boolean getAllowOX() {
            return m_bAllow[0];
        }
        public boolean getAllowPM25(){
            return  m_bAllow[1];
        }
        public boolean getAllowWS(){
            return m_bAllow[2];
        }

        public boolean isAllow() {
            for (int i = 0; i < 3; i++) {
                if (m_bAllow[i]) {
                    return true;
                }
            }
            return false;
        }
    }

    // そらまめの測定データクラス
    public class SoramameData {
        private GregorianCalendar m_dDate;       // 測定日時 UTCのみのようだ
        // SO2 ppm 二酸化硫黄
        // NO ppm 一酸化窒素
        // NO2 ppm 二酸化窒素
        // NOX ppm 窒素酸化物
        // CO ppm 一酸化炭素
        private float m_fOX;// OX ppm 光化学オキシダント
        // SPM mg/m3 浮遊粒子状物質
        private Integer m_nPM25;    // μg/m3 微小粒子状物質 PM2.5測定値 未計測は-100を設定
        private Integer m_nWD;  // WD 16方位(静穏) 風向 静穏は0、北を1として、時計回りに
        private float m_fWS;      // WS m/s 風速

        SoramameData(String strYear, String strMonth, String strDay, String strHour, String strValue)
        {
            m_dDate = new GregorianCalendar(Integer.valueOf(strYear), Integer.valueOf(strMonth)-1,
                    Integer.valueOf(strDay), Integer.valueOf(strHour), 0);
            // 未計測の場合、"-"が出力される。未計測の別パターンがあった。スペースで、コードを調べると（１２２８８）。
//            if( strValue.codePointAt(0) == 12288 || strValue.codePointAt(0) == 160 || strValue.equalsIgnoreCase("-") ){ m_nPM25 = -100 ; }
//            else{ m_nPM25 = Integer.valueOf(strValue); }

            try{
                m_nPM25 = Integer.parseInt(strValue);
            }
            catch(NumberFormatException e){
                e.getMessage();
                m_nPM25 = -100;
            }
        }
        SoramameData(String strYear, String strMonth, String strDay, String strHour, String strOX, String strPM25, String strWD, String strWS)
        {
            // 月は０から11で表現する。取得時も。
            m_dDate = new GregorianCalendar(Integer.valueOf(strYear), Integer.valueOf(strMonth)-1,
                    Integer.valueOf(strDay), Integer.valueOf(strHour), 0);
            // 未計測の場合、"-"が出力される。他のパターンもあった。
//            if( strValue.codePointAt(0) == 12288 || strValue.equalsIgnoreCase("-") ){ m_nPM25 = -100 ; }
//            else{ m_nPM25 = Integer.valueOf(strValue); }
            m_fOX = -0.1f;
            m_nPM25 = -100;
            m_nWD = -1;
            m_fWS = 0.0f;
            try{
                // データを取得できる確率が高い順
                m_nWD = parseWD(strWD);
                m_nPM25 = getValue(strPM25, -100);
                m_fOX = getValue(strOX, -0.1f);
                m_fWS = getValue(strWS, -0.1f);
            }
            // getValue()内にて例外キャッチしているので以下は不要。
            catch(NumberFormatException e){
                e.printStackTrace();
            }
        }

        SoramameData(GregorianCalendar date, float fOX, Integer nPM25, Integer nWD, float fWS){
            //m_dDate = new GregorianCalendar(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.HOUR_OF_DAY), 0, 0);
            m_dDate = date ;
            m_fOX = fOX;
            m_nPM25 = nPM25;
            m_nWD = nWD;
            m_fWS = fWS;
        }

        public GregorianCalendar getDate()
        {
            return m_dDate;
        }
        public String getDateString()
        {
            return String.format("%s/%s/%s %s時",
                    m_dDate.get(Calendar.YEAR), m_dDate.get(Calendar.MONTH)+1, m_dDate.get(Calendar.DAY_OF_MONTH), m_dDate.get(Calendar.HOUR_OF_DAY));
        }
        public String getCalendarString(){
            return String.format("%s/%s/%s %s時",
                    m_dDate.get(Calendar.YEAR), m_dDate.get(Calendar.MONTH)+1, m_dDate.get(Calendar.DAY_OF_MONTH), m_dDate.get(Calendar.HOUR_OF_DAY));
        }
//        public String getDateString(){
//            return String.format("%s/%s/%s",
//   /                 m_dDate.get(Calendar.YEAR), m_dDate.get(Calendar.MONTH)+1, m_dDate.get(Calendar.DAY_OF_MONTH));
//        }
        public String getHourString(){
            return String.format("%d時", m_dDate.get(Calendar.HOUR_OF_DAY));
        }
        public float getOX(){ return m_fOX; }
        public String getOXString(){
            return (m_fOX < 0.0f ? "未計測" : String.format("%.2f", m_fOX ));
        }
        public  Integer getPM25()
        {
            return (m_nPM25 < 0 ? 0 : m_nPM25);
        }
        public double getPM25Radius(){ return (double)getPM25()*100; }
        public String getPM25String(){ return String.format("%s",(m_nPM25 < 0 ? "未計測" : m_nPM25.toString()));}

        public Integer getWD(){ return m_nWD; }
        public float getWDRotation(){
            return (22.5f*(m_nWD-1));
        }
        public float getWS(){ return m_fWS; }
        public String getWSString(){
            return ( m_fWS < 0.0f ? "未計測" : String.format("%.1f", m_fWS));
        }
        // 色設定
        // ～１０　青
        // １１～１５    水色
        // １６～３５    黄緑
        // ３６～５０    黄色
        // ５１～７０    オレンジ
        // ７１～          赤
        public int getPM25Color(){
            int nColor = Color.argb(126, 255, 0,0);
            if( m_nPM25 <= 10 ){
                nColor = Color.argb( 126, 0, 0, 255);
            }
            else if( 11 <= m_nPM25 && m_nPM25 <= 15 ){
                nColor = Color.argb(126, 0, 255, 255);
            }
            else if( 16 <= m_nPM25 && m_nPM25 <= 35 ){
                nColor = Color.argb(126, 0, 255, 128);
            }
            else if( 36 <= m_nPM25 && m_nPM25 <= 50 ){
                nColor = Color.argb(126, 255, 255, 0);
            }
            else if( 51 <= m_nPM25 && m_nPM25 <= 70 ){
                nColor = Color.argb(126, 255, 128, 0);
            }

            return nColor;
        }

        public void setPM25(Integer pm25)
        {
            m_nPM25 = pm25;
        }

        public String Format()
        {
            return String.format("%s:%s %sm", getDateString(), getPM25String(), getWSString()) ;
        }
    }

    private SoramameStation m_Station;                  // 測定局データ
    private ArrayList< SoramameData > m_aData;  // 計測データ
    private String m_strSaisin ;
    private LatLng mPosition;

    @Override
    public int describeContents(){
        return 0;
    }

    // Parcel(小包)用、IntentにてMainActivityからDisplayMessageActivityへデータを渡すため
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(m_Station.getCode());
        out.writeString(m_Station.getName());
        out.writeString(m_Station.getAddress());
        out.writeBooleanArray(m_Station.getAllow());
    }

    public static final Parcelable.Creator<Soramame> CREATOR
            = new Parcelable.Creator<Soramame>() {
        public Soramame createFromParcel(Parcel in) {
            return new Soramame(in);
        }

        public Soramame[] newArray(int size) {
            return new Soramame[size];
        }
    };

    public Soramame(){
        super();
    }

    private Soramame(Parcel in) {
        m_Station = new SoramameStation(in.readInt(), in.readString(), in.readString());
        boolean bFlag[] = new boolean[3];
        in.readBooleanArray(bFlag);
        m_Station.setAllow(bFlag);
        m_aData  = null;
    }

    Soramame(int nCode, String strName, String strAddress)
    {
        m_Station = new SoramameStation(nCode, strName, strAddress);
        m_aData = null ;
    }

    public Integer getMstCode()
    {
        return m_Station.getCode();
    }
    public String getMstName()
    {
        return m_Station.getName();
    }
    public String getAddress()
    {
        return m_Station.getAddress();
    }
    public String getSaisin(){ return m_strSaisin;}
    public LatLng getPosition(){ return mPosition;}

    public void setSaisin(String string){m_strSaisin = string;}
    public void setPosition(LatLng pos){ mPosition = pos; }

    public void setData(String strDate, String strOX, String strPM25, String strWD, String strWS ){
        setData(strDate.substring(0, 4), strDate.substring(4,6), strDate.substring(6,8), strDate.substring(8,10), strOX, strPM25, strWD, strWS );
    }
//    public void setData(String strYear, String strMonth, String strDay, String strHour, String strValue)
   // {
//        SoramameData data = new SoramameData(strYear, strMonth, strDay, strHour, strValue);
//        if( m_aData == null){
//            m_aData = new ArrayList<SoramameData>();
//        }
//        m_aData.add(data);
//    }
    // Set
    public void setData(String strYear, String strMonth, String strDay, String strHour, String strOX, String strPM25, String strWD, String strWS)
    {
        SoramameData data = new SoramameData(strYear, strMonth, strDay, strHour, strOX, strPM25, strWD, strWS);
        addData(data);
    }

    public void setData(SoramameData orig){
        SoramameData data = new SoramameData(orig.getDate(), orig.getOX(), orig.getPM25(), orig.getWD(), orig.getWS());
        addData(data);
    }

    public boolean setAllow(String strOX, String strPM25, String strWD){
        boolean flag[] = new boolean[3];
        int code[] = new int[3];
        code[0] = strOX.codePointAt(0);
        code[1] = strPM25.codePointAt(0);
        code[2] = strWD.codePointAt(0);
        for(int i=0; i<3; i++){
            flag[i] = false;
            if(code[i] == 9675){
                flag[i] = true;
            }
        }
        m_Station.setAllow(flag);
        return m_Station.isAllow();
    }

    public boolean getAllow(int index){
        boolean flag = false;
        switch(index){
            case 0:
                flag = m_Station.getAllowOX();
                break;
            case 1:
                flag = m_Station.getAllowPM25();
                break;
            case 2:
                flag = m_Station.getAllowWS();
                break;
        }
        return flag;
    }

    public void clearData(){
        if( m_aData != null){
            m_aData.clear();
        }
    }

    private void addData(SoramameData data){
        if( m_aData == null){
            m_aData = new ArrayList<SoramameData>();
        }
        m_aData.add(data);
    }

    public String getStationInfo()
    {
        return m_Station.getString();
    }
    public String getDataString(int nIndex)
    {
        if( getSize() < 1){ return ""; }

        return m_aData.get(nIndex).Format() ;
    }
    public SoramameData getData(int nIndex)
    {
        if( getSize() < 1){ return null; }

        return m_aData.get(nIndex) ;
    }
    public int getSize()
    {
        int nSize = 0;
        if(m_aData != null){ nSize = m_aData.size(); }
        return nSize;
    }

    public ArrayList<SoramameData> getData()
    {
        return m_aData;
    }

    // 風向文字列->インデックス変換
    // 静穏 0/北 1/北北東 2/北東 3 -> 北北西 16
    public Integer parseWD(String strWD){
        Integer nWD = -1;
        int start = 0;
        do {
            if (strWD.startsWith("北", start)) {
                // 東北東、西北西
                if(start == 1){
                    if(nWD == 5){ nWD = 4; }
                    else if(nWD == 13){ nWD = 14; }
                }
                else{
                    nWD = 1;
                }
            } else if (strWD.startsWith("南", start)) {
                // 東南東、西南西
                if(start == 1){
                    if(nWD == 5){ nWD = 6; }
                    else if(nWD == 13){ nWD = 12; }
                }
                else {
                    nWD = 9;
                }
            }
            else if(strWD.startsWith("東", start)){
                // 北東、南東
                if(start == 1){
                    if(nWD == 1){ nWD = 3; }
                    else{ nWD = 7; }
                }
                // 北北東、南南東
                else if(start == 2){
                    if(nWD == 1){ nWD = 2; }
                    else if(nWD == 9){ nWD = 8; }
                }
                else {
                    nWD = 5;
                }
            }
            else if(strWD.startsWith("西", start)){
                // 北西、南西
                if(start == 1){
                    if(nWD == 1){ nWD = 15; }
                    else{ nWD = 11; }
                }
                // 北北西、南南西
                else if(start == 2){
                    if(nWD == 1){ nWD = 16; }
                    else if(nWD == 9){ nWD = 10; }
                }
                else {
                    nWD = 13;
                }
            }
            // 静穏
            else{
                nWD = 0;
                break ;
            }
        }while(strWD.length() > ++start);

        return nWD;
    }

    public Integer getValue(String strValue, Integer p){
        Integer value;
        value = p;
        try{
            value = Integer.parseInt(strValue);
        }
        catch(NumberFormatException e){
            value = p;
        }
        return value;
    }
    public Float getValue(String strValue, Float p){
        Float value;
        value = p;
        try{
            value = Float.parseFloat(strValue);
        }
        catch(NumberFormatException e){
            value = p;
        }
        return value;
    }
}
