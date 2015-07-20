package com.example.lunarbasesora;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.graphics.Color;
import android.text.method.LinkMovementMethod;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements LocationListener,
        GoogleMap.OnMarkerDragListener, GoogleMap.OnMapLongClickListener {
    // そらまめメインURL
    private static final String SORABASEURL="http://soramame.taiki.go.jp/";
    private static final String SORASUBURL ="MstItiran.php";
    private static final String SORADATAURL = "DataList.php?MstCode=";
    // 指定都道府県の測定局一覧取得
    private static final String SORAPREFURL ="MstItiranFrame.php?Pref=";
    private static final String SORADATAHYOUURL = "DataHyou.php?BlockID=%02d&Time=%s&Pref=%02d";

    //    private static final String TAG = MapsActivity.class.getSimpleName();
    // 更新時間(目安)
    private static final int LOCATION_UPDATE_MIN_TIME = 1000;
    // 更新距離(目安)
    private static final int LOCATION_UPDATE_MIN_DISTANCE = 100;

    private LocationManager mLocationManager;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private String m_strCurrentPref;
    private LatLng mHome = null;
    private Marker mHomeMarker;

//    /**
//     * Alternative radius for convolution
//     */
//    private static final int ALT_HEATMAP_RADIUS = 10;
//
//    /**
//     * Alternative opacity of heatmap overlay
//     */
//    private static final double ALT_HEATMAP_OPACITY = 0.4;
//
//    /**
//     * Alternative heatmap gradient (blue -> red)
//     * Copied from Javascript version
//     */
//    private static final int[] ALT_HEATMAP_GRADIENT_COLORS = {
//            Color.argb(0, 0, 255, 255),// transparent
//            Color.argb(255 / 3 * 2, 0, 255, 255),
//            Color.rgb(0x8B, 0xC3, 0x4A),
//            Color.rgb(0xff, 0xEB, 0x38),
//            Color.rgb(0xFF, 0x98, 0),
//            Color.rgb(255, 0, 0)
//    };
//
//    public static final float[] ALT_HEATMAP_GRADIENT_START_POINTS = {
//            0.0f, 0.20f, 0.40f, 0.60f, 0.8f, 1.0f
//    };
//
//    public static final Gradient ALT_HEATMAP_GRADIENT = new Gradient(ALT_HEATMAP_GRADIENT_COLORS,
//            ALT_HEATMAP_GRADIENT_START_POINTS);
//    private HeatmapTileProvider mProvider;
//    private TileOverlay mOverlay;
//
//    private boolean mDefaultGradient = true;
//    private boolean mDefaultRadius = true;
//    private boolean mDefaultOpacity = true;

    // 都道府県コードよりブロックID変換
    public int getBlockID(int nPref){
        int nBlockID = 1;
        switch(nPref){
            // 東北
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                nBlockID = 2;
                break;
            // 関東
            // 首都圏は同じでID=10
            case 8:
                nBlockID = 3;
                break;
            // 東海
            case 21:
            case 22:
            case 23:
            case 24:
                nBlockID = 4;
                break;
            // 中部
            case 15:
                nBlockID = 5;
                break;
            // 九州
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
                nBlockID = 8;
                break;
            case 47:
                nBlockID = 9;
                break;
        }

        return nBlockID;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // 都道府県インデックスを取得
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        m_strCurrentPref = sharedPref.getString("CurrentPref", "");

        mLocationManager = (LocationManager)this.getSystemService(Service.LOCATION_SERVICE);
        requestLocationUpdates();
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public void onPause()
    {
        // 都道府県インデックスを保存する
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("CurrentPref", m_strCurrentPref);
        editor.apply();

        super.onPause();
    }

    // Called when the location has changed.
    @Override
    public void onLocationChanged(Location location) {
//        Log.e(TAG, "onLocationChanged.");
//        showLocation(location);
    }

    // Called when the provider status changed.
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
//        Log.e(TAG, "onStatusChanged.");
        switch (status) {
            case LocationProvider.OUT_OF_SERVICE:
                // if the provider is out of service, and this is not expected to change in the near future.
//                String outOfServiceMessage = provider +"�����O�ɂȂ��Ă��Ď擾�ł��܂���B";
//                showMessage(outOfServiceMessage);
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                // if the provider is temporarily unavailable but is expected to be available shortly.
//                String temporarilyUnavailableMessage = "�ꎞ�I��" + provider + "�����p�ł��܂���B�����������炷���ɗ��p�ł���悤�ɂȂ邩���ł��B";
//                showMessage(temporarilyUnavailableMessage);
                break;
            case LocationProvider.AVAILABLE:
                // if the provider is currently available.
                if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
//                    String availableMessage = provider + "�����p�\�ɂȂ�܂����B";
//                    showMessage(availableMessage);
                    requestLocationUpdates();
                }
                break;
        }
    }

    // Called when the provider is enabled by the user.
    @Override
    public void onProviderEnabled(String provider) {
//        Log.e(TAG, "onProviderEnabled.");
//        String message = provider + "���L��ɂȂ�܂����B";
//        showMessage(message);
        if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            requestLocationUpdates();
        }
    }

    // Called when the provider is disabled by the user.
    @Override
    public void onProviderDisabled(String provider) {
//        Log.e(TAG, "onProviderDisabled.");
        if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
//            String message = provider + "������ɂȂ��Ă��܂��܂����B";
//            showMessage(message);
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        mHome = null;
        mHome = marker.getPosition();
        soraUpdates();
        //onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        onMarkerMoved(marker);
    }

    private void onMarkerMoved(Marker marker) {
        if(mHomeMarker != null) {

        }
    }

    @Override
    public void onMapLongClick(LatLng point) {
        // We know the center, let's place the outline at a point 3/4 along the view.
//        View view = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
//                .getView();
//        LatLng radiusLatLng = mMap.getProjection().fromScreenLocation(new Point(
//                view.getHeight() * 3 / 4, view.getWidth() * 3 / 4));

        // ok create it
//        DraggableCircle circle = new DraggableCircle(point, radiusLatLng);
//        mCircles.add(circle);
        mHome = null;
        mHome = point;
        mHomeMarker.setPosition(mHome);
        soraUpdates();
    }

    private void requestLocationUpdates() {
//        Log.e(TAG, "requestLocationUpdates()");
        boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isNetworkEnabled) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_MIN_TIME,
                    LOCATION_UPDATE_MIN_DISTANCE,
                    this);
            Location location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                // 現在地取得
                mHome = new LatLng(location.getLatitude(), location.getLongitude());
                soraUpdates();

//                showLocation(location);
            }
        } else {
//            String message = "Network������ɂȂ��Ă��܂��B";
//            showMessage(message);
        }
    }

    private void soraUpdates(){

        try {
            // 現在地の住所取得（都道府県名）
            // Address.getAdminArea()にて都道府県名を取得できる
            Geocoder geo = new Geocoder(this, Locale.JAPAN);
            List<Address> address = geo.getFromLocation(mHome.latitude, mHome.longitude, 1);
            String strPref = address.get(0).getAdminArea();

            // 都道府県名にてそらまめより、測定局リストを取得する
//                    if(!m_strCurrentPref.equalsIgnoreCase(strPref)) {
            m_strCurrentPref = strPref;
            new Pref().execute(strPref);
//                    }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

//    private void showLocation(Location location) {
//        double longitude = location.getLongitude();
//        double latitude = location.getLatitude();
//        long time = location.getTime();
//        Date date = new Date(time);
//        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
//        String dateFormatted = formatter.format(date);
//        TextView longitudeTextView = (TextView)findViewById(R.id.longitude);
//        longitudeTextView.setText("Longitude : " + String.valueOf(longitude));
//        TextView latitudeTextView = (TextView)findViewById(R.id.latitude);
//        latitudeTextView.setText("Latitude : " + String.valueOf(latitude));
//        TextView geoTimeTextView = (TextView)findViewById(R.id.geo_time);
//        geoTimeTextView.setText("�擾���� : " + dateFormatted);
//    }

//    private void showMessage(String message) {
//        TextView textView = (TextView)findViewById(R.id.message);
//        textView.setText(message);
//    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    //LatLng mStation;
    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
//        try {
//            Geocoder geo = new Geocoder(this, Locale.JAPAN);
//            List<Address> address = geo.getFromLocationName("�k��B�s�ᏼ��厚���ۂT�Ԓn", 1);
//
//            mStation = new LatLng(address.get(0).getLatitude(), address.get(0).getLongitude());

        mMap.setMyLocationEnabled(true);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mHome, 11));

        mHomeMarker = mMap.addMarker(new MarkerOptions()
                .position(mHome)
                .draggable(true)
                .title("I'm Here!"));
//            mMap.addMarker(new MarkerOptions().position(mStation).title(m_strInfo));
//        }
//        catch (IOException e)
//        {
//            return;
//        }
    }

    // 測定局取得
    private class SoraStation extends AsyncTask<Integer, Void, Void>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Integer... params)
        {
            try {

                String url = String.format("%s%s%d", SORABASEURL, SORAPREFURL, params[0]);
                Document doc = Jsoup.connect(url).get();
                Elements prefs = doc.getElementsByAttributeValue("name", "Hyou");
                for (Element pref : prefs) {
                    if (pref.hasAttr("src")) {
                        url = pref.attr("src");
                        String soraurl = SORABASEURL + url;

                        Document sora = Jsoup.connect(soraurl).get();
                        Element body = sora.body();
                        Elements tables = body.getElementsByTag("tr");
                        url = "";
                        Integer cnt = 0;
//                        if (mList != null) {
//                            mList.clear();
//                        }
//                        mList = new ArrayList<Soramame>();

                        for (Element ta : tables) {
                            if (cnt++ > 0) {
                                Elements data = ta.getElementsByTag("td");
                                String kyoku = data.get(13).text();
                                // 最後のデータが空なので
                                if (kyoku.length() < 1) {
                                    break;
                                }
                                int nCode = kyoku.codePointAt(0);
                                // PM2.5測定局のみ
                                if (nCode == 9675) {
                                    Soramame mame = new Soramame(Integer.valueOf(data.get(0).text()), data.get(1).text(), data.get(2).text());
//                                    mame.setSaisin(m_strSaisin);

                                    Geocoder geo = new Geocoder(MapsActivity.this, Locale.JAPAN);
                                    List<Address> address = geo.getFromLocationName(mame.getAddress(), 1);
                                    // 住所から緯度経度が取得できない場合があった
                                    if (address.size() > 0) {

                                        LatLng station = new LatLng(address.get(0).getLatitude(), address.get(0).getLongitude());
                                        mame.setPosition(station);

                                        float[] results = new float[1];
                                        Location.distanceBetween(mHome.latitude, mHome.longitude, station.latitude, station.longitude, results);
                                        if (results[0] < 30000) {
                                            // 該当測定局データを取得
                                            url = String.format("%s%s%d", SORABASEURL, SORADATAURL, mame.getMstCode());
                                            Document sta = Jsoup.connect(url).get();
                                            Elements datas = sta.getElementsByAttributeValue("name", "Hyou");
                                            for (Element dat : datas) {
                                                if (dat.hasAttr("src")) {
                                                    url = dat.attr("src");

                                                    sta = Jsoup.connect(SORABASEURL + url).get();
                                                    Element DataListHyou = sta.body();
                                                    Elements trs = DataListHyou.getElementsByTag("tr");
                                                    Elements tds = trs.get(1).getElementsByTag("td");

                                                    mame.setData(tds.get(0).text(), tds.get(1).text(), tds.get(2).text(), tds.get(3).text(), tds.get(14).text());

//                                                    mList.add(mame);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {

        }

    }

    // 都道府県
    // 入力：現在地の都道府県名
    private class Pref extends AsyncTask<String, Void, Void>
    {
        String m_strSaisin = "";
        int m_nPref = 0;
        ArrayList<String> mStationList;
        ArrayList<Soramame> mList;
        private ProgressDialog mProgDialog;
        SoramameSQLHelper mDbHelper = new SoramameSQLHelper(MapsActivity.this);
        SQLiteDatabase mDb = null;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            if(mProgDialog == null){
                mProgDialog = new ProgressDialog(MapsActivity.this);
                mProgDialog.setTitle("測定局データ");
                mProgDialog.setMessage("ロード中・・・");
                mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mProgDialog.show();
            }
        }

        @Override
        protected Void doInBackground(String... params)
        {
            try
            {

                String url = String.format("%s%s", SORABASEURL, SORASUBURL);
                Document doc = Jsoup.connect(url).get();
                // 最新（日時）取得
                Elements tags = doc.getElementsByTag("input");
                for(Element ele: tags)
                {
                    if( ele.attr("name").equalsIgnoreCase("Saisin"))
                    {
                        m_strSaisin = ele.attr("value");
                        break;
                    }
                }

                Elements elements = doc.getElementsByTag("option");
                for( Element element : elements) {
                    if (Integer.valueOf(element.attr("value")) != 0) {
                        if(element.text().equalsIgnoreCase( params[0] ))
                        {
                            m_nPref = Integer.parseInt(element.attr("value"));
                            // 測定日時の最新と都道府県が分かった時点で、測定局の最新データを読み込んでおく
                            // SORADATAHYOUURLにブロックID、日時、都道府県番号を設定して、URL実行。
                            url = String.format("%sDataMap.php?BlockID=%02d", SORABASEURL, getBlockID(m_nPref));
                            Document date = Jsoup.connect(url).get();
                            Elements dates = date.getElementsByAttributeValue("name", "SaisinTime");
                            m_strSaisin = dates.attr("value");

                            // frame name=Hyouでsrcを取得、そのURLで実行して、測定局のデータを取得
                            getAreaData(m_strSaisin, m_nPref);

                            mDb = mDbHelper.getReadableDatabase();
                            if( !mDb.isOpen() ){ return null; }

                            String[] selectionArgs = { String.valueOf(m_nPref)};
                            Cursor c = mDb.query(SoramameContract.FeedEntry.TABLE_NAME, null,
                                    SoramameContract.FeedEntry.COLUMN_NAME_PREFCODE + " = ?",  selectionArgs, null, null, null);
                            if( c.getCount() > 0 )
                            {
                                if( c.moveToFirst() ) {
                                    if(mList != null) {
                                        mList.clear();
                                    }
                                    mList = new ArrayList<Soramame>();
                                    while (true) {
                                        Soramame mame = new Soramame(
                                                c.getInt(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_CODE)),
                                                c.getString(c.getColumnIndexOrThrow( SoramameContract.FeedEntry.COLUMN_NAME_STATION)),
                                                c.getString(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS)));
                                        LatLng pos = new LatLng( c.getDouble(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_LAT)),
                                                c.getDouble(c.getColumnIndexOrThrow(SoramameContract.FeedEntry.COLUMN_NAME_LNG)));
                                        mame.setPosition(pos);
                                        getStationData(mame);

                                        if( !c.moveToNext()){ break; }
                                    }
                                }
                                c.close();
                                mDb.close();
                                return null;
                            }
                            c.close();
                            mDb.close();

                            mDb = mDbHelper.getWritableDatabase();

                            url = String.format("%s%s%d", SORABASEURL, SORAPREFURL, m_nPref);
                            doc = Jsoup.connect(url).get();
                            Elements prefs = doc.getElementsByAttributeValue("name", "Hyou");
                            for( Element pref : prefs)
                            {
                                if( pref.hasAttr("src")) {
                                    url = pref.attr("src");
                                    String soraurl = SORABASEURL + url;

                                    Document sora = Jsoup.connect(soraurl).get();
                                    Element body = sora.body();
                                    Elements tables = body.getElementsByTag("tr");
                                    url = "";
                                    Integer cnt = 0;
                                    if(mList != null) {
                                        mList.clear();
                                    }
                                    mList = new ArrayList<Soramame>();

                                    for( Element ta : tables) {
                                        if( cnt++ > 0) {
                                            Elements data = ta.getElementsByTag("td");
                                            String kyoku = data.get(13).text();
                                            // 最後のデータが空なので
                                            if(kyoku.length() < 1)
                                            {
                                                break;
                                            }
                                            int nCode = kyoku.codePointAt(0);
                                            // PM2.5測定局のみ
                                            if( nCode == 9675 ) {
                                                Soramame mame = new Soramame(Integer.valueOf(data.get(0).text()), data.get(1).text(), data.get(2).text());
                                                mame.setSaisin(m_strSaisin);

                                                Geocoder geo = new Geocoder(MapsActivity.this, Locale.JAPAN);
                                                List<Address> address = geo.getFromLocationName(mame.getAddress(), 1);
                                                // 住所から緯度経度が取得できない場合があった
                                                if(address.size() > 0) {

                                                    LatLng station = new LatLng(address.get(0).getLatitude(), address.get(0).getLongitude());
                                                    mame.setPosition(station);

                                                    // 測定局DBに保存
                                                    ContentValues values = new ContentValues();
                                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_STATION, data.get(1).text());
                                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_CODE, Integer.valueOf(data.get(0).text()));
                                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_ADDRESS, data.get(2).text());
                                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_PREFCODE, m_nPref);
                                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_LAT, address.get(0).getLatitude());
                                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_LNG, address.get(0).getLongitude());
//                                                    values.put(SoramameContract.FeedEntry.COLUMN_NAME_PM25, );
                                                    // 重複は追加しない
                                                    long newRowId = mDb.insertWithOnConflict(SoramameContract.FeedEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

                                                    getStationData(mame);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
            catch (SQLiteException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            if( mDb.isOpen()){ mDb.close(); }

            Iterator<Soramame> ite = mList.iterator();
            Soramame sora;
            // 以下のコメントはヒートマップ用
             //ArrayList<WeightedLatLng> aList = new ArrayList<WeightedLatLng>();
             while (ite.hasNext()) {
                 sora = ite.next();
                 // 以下のコメントはヒートマップ用
                 //WeightedLatLng weight = new WeightedLatLng( sora.getPosition(), sora.getData(0).getPM25());
                 //aList.add(weight);
                 mMap.addMarker(new MarkerOptions().position(sora.getPosition()).title(sora.getMstName()).snippet(sora.getDataString(0)));
                 mMap.addCircle(new CircleOptions()
                         .center(sora.getPosition())
                         .radius(sora.getData(0).getPM25Radius())
                         .fillColor(sora.getData(0).getPM25Color())
                         .strokeWidth(0));
            }
            // 以下のコメントはヒートマップ用
//            // Check if need to instantiate (avoid setData etc twice)
//            if (mProvider == null) {
//                mProvider = new HeatmapTileProvider.Builder().weightedData(aList).build();
//                mProvider.setRadius(50);
//                mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
//            } else {
//                mProvider.setWeightedData(aList);
//                mOverlay.clearTileCache();
//            }
            mList.clear();
            mProgDialog.dismiss();

        }

        // 指定都道府県最新データ取得
        private void getAreaData(String strSaisin, int nPref){
            // 都道府県一覧データ
            String strSubUrl = String.format(SORADATAHYOUURL, getBlockID(nPref), strSaisin, nPref);
            String url = String.format("%s%s", SORABASEURL, strSubUrl);
            try {
                Document sta = Jsoup.connect(url).get();
                Elements datas = sta.getElementsByAttributeValue("name", "Hyou");
                for (Element dat : datas) {
                    if (dat.hasAttr("src")) {
                        url = dat.attr("src");

                        mStationList = new ArrayList<String>();
                        // 指定都道府県の測定局データ
                        sta = Jsoup.connect(SORABASEURL + url).get();
                        Element DataListHyou = sta.body();
                        Elements trs = DataListHyou.getElementsByTag("tr");
                        //String strCode;
                        //String strValue;
                        for( Element st : trs){
                            Elements tds = st.getElementsByTag("td");
                            mStationList.add(tds.get(0).text());        // 測定局コード
                            mStationList.add(tds.get(13).text());      // PM2.5
                        }
                        break;
                    }
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        private void getStationData(Soramame mame)
        {
            float[] results = new float[1];
            Location.distanceBetween(mHome.latitude, mHome.longitude, mame.getPosition().latitude, mame.getPosition().longitude, results);
            if( results[0] < 30000) {
                Iterator<String> ite = mStationList.iterator();
                while(ite.hasNext()){
                    if(ite.next().equalsIgnoreCase(mame.getMstCode().toString())){
                        mame.setData(m_strSaisin, ite.next());
                        mList.add(mame);
                        break;
                    }
                    ite.next();
                }
//                // 該当測定局データを取得
//                String url = String.format("%s%s%d", SORABASEURL, SORADATAURL, mame.getMstCode());
//                try {
//                    Document sta = Jsoup.connect(url).get();
//                    Elements datas = sta.getElementsByAttributeValue("name", "Hyou");
//                    for (Element dat : datas) {
//                        if (dat.hasAttr("src")) {
//                            url = dat.attr("src");
//
//                            sta = Jsoup.connect(SORABASEURL + url).get();
//                            Element DataListHyou = sta.body();
//                            Elements trs = DataListHyou.getElementsByTag("tr");
//                            Elements tds = trs.get(1).getElementsByTag("td");
//
//                            mame.setData(tds.get(0).text(), tds.get(1).text(), tds.get(2).text(), tds.get(3).text(), tds.get(14).text());
//
//                            mList.add(mame);
//                            break;
//                        }
//                    }
//                }
//                catch(IOException e)
//                {
//                    e.printStackTrace();
//                }
            }
        }
    }

    // 測定局コードにて最新の測定値を取得する
    // これはユーザーが指定した測定局データ取得用とする。
    private class MstData extends AsyncTask<Soramame, Void, Soramame>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Soramame doInBackground(Soramame... params) {
            // 該当測定局データを取得
            String url = String.format("%s%s%d", SORABASEURL, SORADATAURL, params[0].getMstCode());
            try {
                Document sta = Jsoup.connect(url).get();
                Elements datas = sta.getElementsByAttributeValue("name", "Hyou");
                for (Element dat : datas) {
                    if (dat.hasAttr("src")) {
                        url = dat.attr("src");

                        sta = Jsoup.connect(SORABASEURL + url).get();
                        Element DataListHyou = sta.body();
                        Elements trs = DataListHyou.getElementsByTag("tr");
                        Elements tds = trs.get(1).getElementsByTag("td");

                        params[0].setData(tds.get(0).text(), tds.get(1).text(), tds.get(2).text(), tds.get(3).text(), tds.get(14).text());

                        break;
                    }
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            return params[0];
        }

        @Override
        protected void onPostExecute(Soramame result) {
        }

    }
}
