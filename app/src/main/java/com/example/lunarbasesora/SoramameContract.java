package com.example.lunarbasesora;

import android.provider.BaseColumns;

/**
 * Created by on 2015/07/12.
 */
public final class SoramameContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public SoramameContract() {}

    /* Inner class that defines the table contents */
    // 測定局名称、コード、住所、緯度経度
    public static abstract class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "soramamestation";
        public static final String COLUMN_NAME_STATION = "stationname";
        public static final String COLUMN_NAME_CODE = "stationcode";
        public static final String COLUMN_NAME_ADDRESS = "stationaddress";
        public static final String COLUMN_NAME_PREFCODE = "prefcode";
        public static final String COLUMN_NAME_LAT ="stationLat";
        public static final String COLUMN_NAME_LNG = "stationLng";
        public static final String COLUMN_NAME_PM25 = "PM25";
    }
}