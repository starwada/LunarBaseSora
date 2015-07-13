package com.example.lunarbasesora;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Date;


/**
 * そらまめデータ
 * Created by Wada on 2015/07/03.
 */
public class Soramame {
    // そらまめの測定局データ
    public  class SoramameStation
    {
        private int m_nCode;                // 測定局コード
        private String m_strName;       // 測定局名称
        private String m_strAddress;    // 住所

        public SoramameStation(int nCode, String strName, String strAddress)
        {
            setCode(nCode);
            setName(strName);
            setAddress(strAddress);
        }
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
    }

    // そらまめの測定データクラス
    public class SoramameData {
        private Date m_dDate;       // 測定日時 UTCのみのようだ
        private Integer m_nPM25;    // PM2.5測定値 未計測は-100を設定

        SoramameData(String strYear, String strMonth, String strDay, String strHour, String strValue)
        {
            m_dDate = new Date(new Integer(strYear), new Integer(strMonth),
                    new Integer(strDay), new Integer(strHour), new Integer(0));
            // 未計測の場合、"-"が出力される。
            if( strValue.equalsIgnoreCase("-") ){ m_nPM25 = -100 ; }
            else{ m_nPM25 = new Integer(strValue); }
        }

        public Date getDate()
        {
            return m_dDate;
        }
        public String getDateString()
        {
            return String.format("%s/%s/%s %s時",
                    m_dDate.getYear(), m_dDate.getMonth(), m_dDate.getDate(), m_dDate.getHours());
        }
        public  Integer getPM25()
        {
            return m_nPM25;
        }
        public String getPM25String(){ return String.format("%s",(m_nPM25 < 0 ? "未計測" : m_nPM25.toString()));}

        public void setPM25(Integer pm25)
        {
            m_nPM25 = pm25;
        }

        public String Format()
        {
            String strFormat = String.format("%s:%s", getDateString(), getPM25String()) ;
            return strFormat ;
        }
    }

    private SoramameStation m_Station;                  // 測定局データ
    private ArrayList< SoramameData > m_aData;  // 計測データ
    private String m_strSaisin ;
    private LatLng mPosition;

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

    public void setData(String strYear, String strMonth, String strDay, String strHour, String strValue)
    {
        SoramameData data = new SoramameData(strYear, strMonth, strDay, strHour, strValue);
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
        return m_aData.size();
    }

    public ArrayList<SoramameData> getData()
    {
        return m_aData;
    }
}
