package com.nexuslink.alphrye.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.help.Tip;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkRouteResult;
import com.nexuslink.alphrye.common.MyActivity;
import com.nexuslink.alphrye.cyctastic.R;
import com.nexuslink.alphrye.helper.AMapUtil;
import com.nexuslink.alphrye.helper.MyLogUtil;
import com.nexuslink.alphrye.helper.RideRouteOverlay;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.OnClick;

import static com.nexuslink.alphrye.ui.activity.HomeActivity.REQUEST_SEARCH_TIP;

/**
 * 地图定位页面
 * @author yuanrui
 * @date 2019/4/12
 */
public class MapActivity extends MyActivity {

    /**
     * 日志Tag
     */
    public static final String TAG = "MapActivity";

    /**
     * 默认缩放大小
     */
    public static final float DEFAULT_ZOOM_LEVEL = 17.0f;

    /**
     * GPS定位精度圆圈描边
     */
    public static final String GPS_CIRCLE_COLOR_STOCK = "#00000000";

    /**
     * GPS定位精度圆圈填充
     */
    public static final String GPS_CIRCLE_COLOR_FILL = "#00000000";

    /**
     * 地图视图
     */
    private MapView mMapView;

    /**
     * Amap实例(地图控制器)
     */
    private AMap mAmap;

    /**
     * 定位样式控制
     */
    private MyLocationStyle myLocationStyle;

    /**
     * 定位请求client
     */
    private AMapLocationClient mLocationClient;

    /**
     * 定位设置
     */
    private AMapLocationClientOption mLocationOption;

    /**
     * 路线搜索
     */
    private RouteSearch mRouteSearch;

    /**
     * 当前坐标(维度)
     */
    private double mCurLatitude;

    /**
     * 当前坐标(经度)
     */
    private double mCurLongitude;

    @BindView(R.id.tv_location)
    TextView mTvLocation;

    @BindView(R.id.tv_location_data)
    TextView mTvLocationData;

    @BindView(R.id.tv_status)
    TextView mTvStatus;

    /**
     * 搜索事件
     */
    @OnClick(R.id.v_search) void onSearchClick (){
        Intent intent = new Intent(this, SearchActivity.class);
        startActivityForResult(intent, REQUEST_SEARCH_TIP);
    }

    /**
     * 自定义放大缩小Map
     */
    @OnClick({R.id.btn_zoom_large, R.id.btn_zoom_small}) void onZoomClick(View view) {
        CameraPosition cameraPosition = mAmap.getCameraPosition();
        //获取当前缩放等级
        float curZoomLevel = cameraPosition.zoom;
        //获取目标位置
        LatLng nowLocation = cameraPosition.target;
        boolean isZoomLarge = view.getId() == R.id.btn_zoom_large;
        mAmap.animateCamera(CameraUpdateFactory.newLatLngZoom(nowLocation, isZoomLarge ? ++curZoomLevel : --curZoomLevel));
    }

    @OnClick(R.id.btn_cur_loacation) void onCurLocationClick() {
        mAmap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurLatitude, mCurLongitude), DEFAULT_ZOOM_LEVEL));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMapView = findViewById(R.id.v_map);
        mMapView.onCreate(savedInstanceState);

        //AMap设置
        mAmap = mMapView.getMap();
        //启动定位蓝点
        mAmap.setMyLocationEnabled(true);

        //定位蓝点样式控制
        myLocationStyle = new MyLocationStyle();
        /*
          定位类型:
          MyLocationStyle.LOCATION_TYPE_SHOW: 只定位一次。
          MyLocationStyle.LOCATION_TYPE_LOCATE: 定位一次，且将视角移动到地图中心点。
          MyLocationStyle.LOCATION_TYPE_FOLLOW: 连续定位、且将视角移动到地图中心点，定位蓝点跟随设备移动。（1秒1次定位）
          MyLocationStyle.LOCATION_TYPE_MAP_ROTATE: 连续定位、且将视角移动到地图中心点，地图依照设备方向旋转，定位点会跟随设备移动。（1秒1次定位）
          MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE: 连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）默认执行此种模式
        */
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        //设置精度圆圈填充颜色为透明
        myLocationStyle.radiusFillColor(Color.parseColor(GPS_CIRCLE_COLOR_STOCK));
        //设置描边颜色为透明
        myLocationStyle.strokeColor(Color.parseColor(GPS_CIRCLE_COLOR_FILL));
        //定位图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
        //设置定位蓝点的Style
        mAmap.setMyLocationStyle(myLocationStyle);
        //设置视角等级
        mAmap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
        //显示交通
        mAmap.setTrafficEnabled(true);
        //显示建筑
        mAmap.showBuildings(true);
        //显示室内地图
        mAmap.showIndoorMap(true);
        //地图上文案显示
        mAmap.showMapText(true);
        //位置变化监听
        mAmap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (location == null) {
                    MyLogUtil.d("TAG", "Amap onMyLocationChange: location is null");
                    return;
                }
                MyLogUtil.d("TAG", "Amap onMyLocationChange: location :( Altitude "
                        + location.getAltitude() + ", Longitude " + location.getLongitude() + ")");
                mAmap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM_LEVEL));
            }
        });

        //地图UI设置
        UiSettings uiSettings = mAmap.getUiSettings();
        if (uiSettings != null) {
            //设置默认定位按钮是否显示，非必需设置。
            uiSettings.setMyLocationButtonEnabled(false);
            uiSettings.setZoomControlsEnabled(false);
            uiSettings.setCompassEnabled(false);
            uiSettings.setScaleControlsEnabled(true);
            uiSettings.setLogoPosition(AMapOptions.LOGO_POSITION_BOTTOM_LEFT);
            //手势交互
            uiSettings.setAllGesturesEnabled(true);
        }

        //定位请求
        mLocationOption = new AMapLocationClientOption();
        //设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.Sport);
        /*
          定位模式：
          Device_Sensors(仅用设备定位模式)
          Hight_Accuracy(高精度定位模式)
          Battery_Saving(低功耗定位模式)
        */
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationClient = new AMapLocationClient(getContext());
        mLocationClient.setLocationOption(mLocationOption);
        mLocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation == null) {
                    return;
                }
                if (aMapLocation.getErrorCode() == 0) {
                    //可在其中解析amapLocation获取相应内容。
                    if (mCurLatitude != aMapLocation.getLatitude()
                            || mCurLongitude != aMapLocation.getLongitude()) {
                        mCurLatitude = aMapLocation.getLatitude();
                        mCurLongitude = aMapLocation.getLongitude();
                    }

                    String locationString = aMapLocation.getDistrict() + aMapLocation.getStreet();
                    if (TextUtils.isEmpty(locationString)) {
                        locationString = "--";
                    }
                    mTvLocation.setText(locationString);
                    String latitudeString = String.format("%.2f", mCurLatitude);
                    String longitudeString = String.format("%.2f", mCurLongitude);
                    if (TextUtils.isEmpty(latitudeString)) {
                        latitudeString = "--";
                    }

                    if (TextUtils.isEmpty(longitudeString)) {
                        latitudeString = "--";
                    }

                    mTvLocationData.setText("维度: " + latitudeString + " 经度: " + longitudeString);
                }else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    MyLogUtil.d(TAG,"location Error, ErrCode:"
                            + aMapLocation.getErrorCode() + ", errInfo:"
                            + aMapLocation.getErrorInfo());
                }
            }
        });
        //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
        mLocationClient.stopLocation();
        mLocationClient.startLocation();

        mRouteSearch = new RouteSearch(getContext());
        mRouteSearch.setRouteSearchListener(new RouteSearch.OnRouteSearchListener() {
            @Override
            public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

            }

            @Override
            public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

            }

            @Override
            public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int i) {

            }

            @Override
            public void onRideRouteSearched(RideRouteResult result, int errorCode) {
                Log.d("Test", "onRideRouteSearched: ");
                mAmap.clear();// 清理地图上的所有覆盖物
                if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                    if (result != null && result.getPaths() != null) {
                        if (result.getPaths().size() > 0) {
                            final RidePath ridePath = result.getPaths()
                                    .get(0);
                            if(ridePath == null) {
                                return;
                            }
                            RideRouteOverlay rideRouteOverlay = new RideRouteOverlay(
                                    getContext(), mAmap, ridePath,
                                    result.getStartPos(),
                                    result.getTargetPos());
                            rideRouteOverlay.removeFromMap();
                            rideRouteOverlay.addToMap();
                            rideRouteOverlay.zoomToSpan();
//                                mBottomLayout.setVisibility(View.VISIBLE);
                            int dis = (int) ridePath.getDistance();
                            int dur = (int) ridePath.getDuration();
                            String des = AMapUtil.getFriendlyTime(dur)+"("+AMapUtil.getFriendlyLength(dis)+")";
//                                mRotueTimeDes.setText(des);
//                                mRouteDetailDes.setVisibility(View.GONE);
//                                mBottomLayout.setOnClickListener(new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {
//                                        Intent intent = new Intent(mContext,
//                                                RideRouteDetailActivity.class);
//                                        intent.putExtra("ride_path", ridePath);
//                                        intent.putExtra("ride_result",
//                                                mRideRouteResult);
//                                        startActivity(intent);
//                                    }
//                                });

                            Intent intent = new Intent(getContext(), RideRouteCalculateActivity.class);
                            intent.putExtra("start_point", result.getStartPos());
                            intent.putExtra("end_point", result.getTargetPos());
                            startActivity(intent);

                        } else if (result != null && result.getPaths() == null) {
//                                ToastUtil.show(mContext, R.string.no_result);
                        }
                    } else {
//                            ToastUtil.show(mContext, R.string.no_result);
                    }
                } else {
//                        ToastUtil.showerror(this.getApplicationContext(), errorCode);
                }
            }
        });

        mTvLocation.setText("--");
        mTvLocationData.setText("维度: -- 维度: --");
        mTvStatus.setText(R.string.locating);
        mTvStatus.setBackgroundColor(Color.parseColor("#1DA1F2"));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_cycle;
    }

    @Override
    protected int getTitleBarId() {
        return R.id.tb_cycle_title;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
//        mAMapNaviView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
//        mAMapNaviView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMapView != null) {
            mMapView.onDestroy();
        }
//        mAMapNaviView.onDestroy();
        //销毁定位客户端，同时销毁本地定位服务。
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_SEARCH_TIP) {
                if (data == null) {
                    return;
                }
                Tip tip = data.getParcelableExtra("search_tip");
                if (tip == null) {
                    return;
                }
                Log.d("Test", "onActivityResult: " + tip.getName());
                final LatLonPoint point = tip.getPoint();
                if (point == null) {
                    return;
                }

                RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(getCurrentPoint(), point);

                RouteSearch.RideRouteQuery query = new RouteSearch.RideRouteQuery(fromAndTo);
                mRouteSearch.calculateRideRouteAsyn(query);
            }
        }
    }

    private LatLonPoint getCurrentPoint() {
        return new LatLonPoint(mCurLatitude, mCurLongitude);
    }
}
