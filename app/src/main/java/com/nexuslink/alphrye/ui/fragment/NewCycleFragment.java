package com.nexuslink.alphrye.ui.fragment;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.amap.api.track.AMapTrackClient;
import com.amap.api.track.ErrorCode;
import com.amap.api.track.OnTrackLifecycleListener;
import com.amap.api.track.TrackParam;
import com.amap.api.track.query.entity.Point;
import com.amap.api.track.query.entity.Track;
import com.amap.api.track.query.model.AddTerminalRequest;
import com.amap.api.track.query.model.AddTerminalResponse;
import com.amap.api.track.query.model.AddTrackRequest;
import com.amap.api.track.query.model.AddTrackResponse;
import com.amap.api.track.query.model.DistanceRequest;
import com.amap.api.track.query.model.DistanceResponse;
import com.amap.api.track.query.model.HistoryTrackResponse;
import com.amap.api.track.query.model.LatestPointRequest;
import com.amap.api.track.query.model.LatestPointResponse;
import com.amap.api.track.query.model.OnTrackListener;
import com.amap.api.track.query.model.ParamErrorResponse;
import com.amap.api.track.query.model.QueryTerminalRequest;
import com.amap.api.track.query.model.QueryTerminalResponse;
import com.amap.api.track.query.model.QueryTrackResponse;
import com.nexuslink.alphrye.SimpleAdapter;
import com.nexuslink.alphrye.SimpleModel;
import com.nexuslink.alphrye.api.EagleApiService;
import com.nexuslink.alphrye.api.HeWeatherApiService;
import com.nexuslink.alphrye.common.CommonConstance;
import com.nexuslink.alphrye.common.MyApplication;
import com.nexuslink.alphrye.common.MyLazyFragment;
import com.nexuslink.alphrye.cyctastic.R;
import com.nexuslink.alphrye.helper.FlashLightHelper;
import com.nexuslink.alphrye.helper.MyLogUtil;
import com.nexuslink.alphrye.helper.SPUtil;
import com.nexuslink.alphrye.helper.SimpleOnTrackLifecycleListener;
import com.nexuslink.alphrye.helper.SimpleOnTrackListener;
import com.nexuslink.alphrye.model.CommonEagleNetModel;
import com.nexuslink.alphrye.model.FlashlightChangeEvent;
import com.nexuslink.alphrye.model.FullServerModel;
import com.nexuslink.alphrye.model.HeWeatherModel;
import com.nexuslink.alphrye.model.RunningDataModel;
import com.nexuslink.alphrye.model.ServerListModel;
import com.nexuslink.alphrye.model.SimpleServerModel;
import com.nexuslink.alphrye.net.wrapper.RetrofitWrapper;
import com.nexuslink.alphrye.ui.activity.HomeActivity;
import com.nexuslink.alphrye.ui.weight.DashboardView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * @author yuanrui
 * @dete 2019/4/12
 */
public class NewCycleFragment extends MyLazyFragment {

    public static final int FLAG_TIME_START = 0;

    public static final int FLAG_TIME_PAUSE = 1;

    public static final int FLAG_TIME_CONTINUE = 2;

    public static final int FLAG_UPDATE_ALTITUDE = 3;

//    public static final int POSITION_KM = 0;

//    public static final int POSITION_CAL = 1;

//    public static final int POSITION_ALTITUDE = 2;

    public static final int POSITION_WEATHER = 0;

    public static final int POSITION_WIND_DIR = 1;

    public static final int POSITION_WIND_ZI = 2;

    public static final int POSITION_VIS = 3;

    public static final int POSITION_PCPN = 4;

    private static final String TAG = "NewCycleFragment";

    private static final String CHANNEL_ID_SERVICE_RUNNING = "CHANNEL_ID_SERVICE_RUNNING";

    private static final int STATUS_READY = 0;

    private static final int STATUS_RUNNING = 1;

    private static final int STATUS_PAUSE = 2;

    private static final int mDataRaw = 3;

    private static final int REFRESH_ALL = 0;

    private static final int UPDATE_SPEED = 1;

    private int curSec;

    private int mCurrentStatus;

    private long terminalId;

    private long trackId;

    private long serverId;

    private long recordTime;

    private boolean isServiceRunning;

    private boolean isGatherRunning;

    private boolean isFlashlightOn;

    private boolean isCountDonw;

    private String mCurTime = "00:00";

    private String mCurBattery = "0%";

    private String mCurTmp = "--";

    private String mWindDir = "--";

    private String mWindSpd = "0";

    private String mVis = "0";

    private String mPcpn = "0";

    private String mPres = "0";

    private SimpleAdapter mSimpleAdapter;

    private List<SimpleModel> modelList;

    private LocationManager mLocationManager;

    private AMapTrackClient mAMapTrackClient;

    private GnssStatus.Callback mGnssStatusCallback;

    private Timer mQueryTimer;

    private TimerTask queryTask;

    private Handler mHandler;

    private TextToSpeech textToSpeech;

    private final float defaultSpeed = 5.0f;

    private boolean isSendMsg = false;

    /**
     * 时间、时区改变广播
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Test", "onReceive: ");
            if (intent == null) {
                return;
            }

            updateBatteryAndTime();
        }
    };

    private OnTrackLifecycleListener onTrackListener = new SimpleOnTrackLifecycleListener() {

        @Override
        public void onBindServiceCallback(int status, String msg) {
            Log.w(TAG, "onBindServiceCallback, status: " + status + ", msg: " + msg);
        }

        @Override
        public void onStartTrackCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.START_TRACK_SUCEE || status == ErrorCode.TrackListen.START_TRACK_SUCEE_NO_NETWORK) {
                // 成功启动
//                Toast.makeText(getContext(), "启动服务成功", Toast.LENGTH_SHORT).show();
                isServiceRunning = true;
                if (!isGatherRunning) {
                    Log.d(TAG, "onStartTrackCallback 1 : trackId = " + trackId);
                    mAMapTrackClient.setTrackId(trackId);
                    mAMapTrackClient.startGather(onTrackListener);
                }
            } else if (status == ErrorCode.TrackListen.START_TRACK_ALREADY_STARTED) {
                // 已经启动
//                Toast.makeText(getContext(), "服务已经启动", Toast.LENGTH_SHORT).show();
                isServiceRunning = true;

                if (!isGatherRunning) {
                    Log.d(TAG, "onStartTrackCallback 2 : trackId = " + trackId);
                    mAMapTrackClient.setTrackId(trackId);
                    mAMapTrackClient.startGather(onTrackListener);
                }
            } else {
                Log.w(TAG, "error onStartTrackCallback, status: " + status + ", msg: " + msg);
                Toast.makeText(getContext(),
                        "error onStartTrackCallback, status: " + status + ", msg: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onStopTrackCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.STOP_TRACK_SUCCE) {
                // 成功停止
//                Toast.makeText(getContext(), "停止服务成功", Toast.LENGTH_SHORT).show();
                isServiceRunning = false;
                isGatherRunning = false;
            } else {
                Log.w(TAG, "error onStopTrackCallback, status: " + status + ", msg: " + msg);
                Toast.makeText(getContext(),
                        "error onStopTrackCallback, status: " + status + ", msg: " + msg,
                        Toast.LENGTH_LONG).show();

            }
        }

        @Override
        public void onStartGatherCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.START_GATHER_SUCEE) {
//                Toast.makeText(getContext(), "定位采集开启成功", Toast.LENGTH_SHORT).show();
                isGatherRunning = true;

                //开启猎鹰采集成功，开启定时器，执行定时查询任务
                mQueryTimer = new Timer();
                queryTask = new TimerTask() {
                    @Override
                    public void run() {
                        queryClick();
                    }
                };
                mQueryTimer.schedule(queryTask, 2000, 2000);
            } else if (status == ErrorCode.TrackListen.START_GATHER_ALREADY_STARTED) {
//                Toast.makeText(getContext(), "定位采集已经开启", Toast.LENGTH_SHORT).show();
                isGatherRunning = true;
            } else {
                Log.w(TAG, "error onStartGatherCallback, status: " + status + ", msg: " + msg);
                Toast.makeText(getContext(),
                        "error onStartGatherCallback, status: " + status + ", msg: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onStopGatherCallback(int status, String msg) {
            if (status == ErrorCode.TrackListen.STOP_GATHER_SUCCE) {
//                Toast.makeText(getContext(), "定位采集停止成功", Toast.LENGTH_SHORT).show();
                isGatherRunning = false;
                mQueryTimer.cancel();
            } else {
                Log.w(TAG, "error onStopGatherCallback, status: " + status + ", msg: " + msg);
                Toast.makeText(getContext(),
                        "error onStopGatherCallback, status: " + status + ", msg: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        }
    };

    @BindView(R.id.btn_done)
    TextView mBtnDone;

    @BindView(R.id.btn_start_or_pause)
    TextView mBtnStartOrPause;

    @BindView(R.id.v_recycler)
    RecyclerView mRecyclerView;

    @BindView(R.id.tv_debug)
    TextView mTvDebug;

    @BindView(R.id.v_dash_board)
    DashboardView mDashboardView;

    @BindView(R.id.v_chronometer)
    Chronometer mChronometer;

    @BindView(R.id.status_flash)
    TextView mFlashLight;

    @BindView(R.id.v_battery)
    TextView mBattery;

    @BindView(R.id.tv_cur_time)
    TextView mTvCurTime;

    @BindView(R.id.beyound_speed)
    TextView mTestBeyoundSpeed;

    @BindView(R.id.v_test)
    LinearLayout mLLTest;


    @OnClick(R.id.status_flash) void onFlashLightClick() {
        if (SPUtil.getBoolean(CommonConstance.SP_STATUS_FLASH, false)) {
            toast("手动管理闪光灯需要先关闭自动闪光灯");
            return;
        }

        isFlashlightOn = !isFlashlightOn;
        FlashLightHelper.getInstance().switchFlashLight(isFlashlightOn, new FlashLightHelper.OnSwitchCallBack() {
            @Override
            public void onSwitch(boolean b) {
                mFlashLight.setText(b?"关":"开");
            }
        });
    }

    @OnClick(R.id.btn_start_or_pause)
    void onStartOrPause() {
        if (mCurrentStatus == STATUS_READY) {
            //开始计时
            onRideStartClick();
        } else if (mCurrentStatus == STATUS_RUNNING) {
            //暂停
            onRidePauseClick();
        } else if (mCurrentStatus == STATUS_PAUSE) {
            //继续
            onRideContinueClick();
        }
    }

    @OnClick(R.id.btn_done)
    void onDone() {
        onRideFinishClick();
    }

    //测试
    @OnClick(R.id.beyound_speed)
    void onBeyoundSpeed() {
        int speed = 5;
        mDashboardView.updateSpeed(speed);
        final boolean isSpeedOn = SPUtil.getBoolean(CommonConstance.SP_STATUS_SPEED, true);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isSpeedOn && !isCountDonw) {
                    startAuto("骑车帮提醒您：请减速慢行，道路千万条，安全第一条。行车不规范，亲人两行泪");
                    isCountDonw = true;
                    curSec = 10;
                    Timer timer = new Timer();
                    TimerTask task = new TimerTask() {
                        @Override
                        public void run() {
                            curSec--;
                            if (curSec == 0) {
                                isCountDonw = false;
                                cancel();
                                mHandler.sendEmptyMessage(UPDATE_SPEED);
                            }
                        }
                    };
                    timer.schedule(task, 0, 1000);
                }
            }
        }, (long) (speed * 3.6 * 100));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_new_cycle;
    }

    @Override
    protected int getTitleBarId() {
        return R.id.tb_cycle_title;
    }

    @Override
    protected void initView() {

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (REFRESH_ALL == msg.what) {
                    if (mSimpleAdapter == null) {
                        return;
                    }
                    mSimpleAdapter.notifyDataSetChanged();
                } else if (UPDATE_SPEED == msg.what){
                    //测试
                    mDashboardView.updateSpeed(0);
                }
            }
        };

        modelList = new ArrayList<>();
//        modelList.add(POSITION_KM, new RunningDataModel("里程(KM)", "0"));
//        modelList.add(POSITION_CAL, new RunningDataModel("热量(CAL)", "0"));
//        modelList.add(POSITION_ALTITUDE, new RunningDataModel("实时海拔(M)", "0"));
        modelList.add(POSITION_WEATHER, new RunningDataModel("气温(摄氏度)", "--"));
        modelList.add(POSITION_WIND_DIR, new RunningDataModel("风向(M/S)", mWindDir));
        modelList.add(POSITION_WIND_ZI, new RunningDataModel("压强(PA)", mPres));
        modelList.add(POSITION_VIS, new RunningDataModel("能见度(M)", mVis));
        modelList.add(POSITION_PCPN, new RunningDataModel("降雨量(MM)", mPcpn));

        mSimpleAdapter = new SimpleAdapter.Builder(getContext())
                .recyclerView(mRecyclerView)
                .layoutManager(new StaggeredGridLayoutManager(mDataRaw, StaggeredGridLayoutManager.VERTICAL))
                .data(modelList)
                .build();

        updateBatteryAndTime();
//        //时间与电量获取
        Timer timer = new Timer();

        TimerTask taskPer30Min = new TimerTask() {
            @Override
            public void run() {
                //温度获取
                RetrofitWrapper.getInstance(CommonConstance.HE_WEATHER_URL)
                        .createService(HeWeatherApiService.class)
                        .nowWeather("CN101040100", CommonConstance.KEY_HE_WEATHER)
                        .enqueue(new Callback<HeWeatherModel>() {
                            @Override
                            public void onResponse(Call<HeWeatherModel> call, Response<HeWeatherModel> response) {
                                if (!response.isSuccessful()) {
                                    return;
                                }
                                HeWeatherModel model = response.body();
                                if (model == null) {
                                    return;
                                }
                                List<HeWeatherModel.HeWeather6Bean> heWeather6 = model.HeWeather6;
                                if (heWeather6 == null || heWeather6.isEmpty()) {
                                    return;
                                }
                                HeWeatherModel.HeWeather6Bean bean = heWeather6.get(0);

                                if (bean == null) {
                                    return;
                                }
                                HeWeatherModel.HeWeather6Bean.NowBean now = bean.now;
                                if (now == null) {
                                    return;
                                }
                                mCurTmp = now.tmp;
                                mWindDir = now.wind_dir;
                                mWindSpd = now.wind_spd;
                                mPres = now.pres;
                                mVis = now.vis;
                                mPcpn = now.pcpn;
                                Log.d(TAG, "onResponse: tmp = " + mCurTmp);
                                RunningDataModel weatherModel = (RunningDataModel) modelList.get(POSITION_WEATHER);
                                if (weatherModel != null) {
                                    weatherModel.mData = mCurTmp;
                                }
                                RunningDataModel windDirModel = (RunningDataModel) modelList.get(POSITION_WIND_DIR);
                                if (windDirModel != null) {
                                    windDirModel.mData = mWindSpd;
                                    windDirModel.mTitle = mWindDir + "(M/S)";
                                }

                                RunningDataModel ziModel = (RunningDataModel) modelList.get(POSITION_WIND_ZI);
                                if (ziModel != null) {
                                    ziModel.mData = mPres;
                                }
                                RunningDataModel visModel = (RunningDataModel) modelList.get(POSITION_VIS);
                                if (visModel != null) {
                                    visModel.mData = mVis;
                                }

                                RunningDataModel pcpnModel = (RunningDataModel) modelList.get(POSITION_PCPN);
                                if (pcpnModel != null) {
                                    pcpnModel.mData = mPcpn;
                                }

                                mSimpleAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(Call<HeWeatherModel> call, Throwable t) {

                            }
                        });
            }
        };
        timer.schedule(taskPer30Min, 0,30 * 60 * 1000);

        IntentFilter filter = new IntentFilter();
        //（每分钟发出的广播）
        filter.addAction(Intent.ACTION_TIME_TICK);
        //时间改变
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        //时区改变
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (getContext() != null) {
            getContext().registerReceiver(mBroadcastReceiver, filter);
        }

        //初始化操作视图状态
        mBtnDone.setVisibility(View.GONE);
        mBtnStartOrPause.setText("开始");
        mCurrentStatus = STATUS_READY;

        if (SPUtil.getBoolean(CommonConstance.SP_STATUS_FLASH, false)) {
            mFlashLight.setText("自动");
        } else {
            mFlashLight.setText("开");
        }
    }

    @Override
    protected void initData() {
        EventBus.getDefault().register(this);
        mAMapTrackClient = new AMapTrackClient(MyApplication.getContext());
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Typeface typeface = Typeface.createFromAsset(MyApplication.getContext().getAssets(), "lcd_num.ttf");
        mChronometer.setTypeface(typeface);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mGnssStatusCallback = new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    super.onStarted();
                    MyLogUtil.d(TAG, "onStarted: ");
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    MyLogUtil.d(TAG, "onStopped: ");
                }

                @Override
                public void onFirstFix(int ttffMillis) {
                    super.onFirstFix(ttffMillis);
                    MyLogUtil.d(TAG, "onFirstFix" );
                }

                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    super.onSatelliteStatusChanged(status);
                    MyLogUtil.d(TAG, "onSatelliteStatusChanged: ");
                }
            };
        } else {
            // TODO: 2019/5/9 Android低版本适配
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //打开GPS
            toast("请开启GPS");
        } else {
            String bestProvider = mLocationManager.getBestProvider(getLocationCriteria(), true);
            //权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
            } else {
                // TODO: 2019/5/9 Android低版本适配
            }
            Location location = mLocationManager.getLastKnownLocation(bestProvider);
            updateAltitudeByLocation(location);
        }

        LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    return;
                }
                MyLogUtil.d(TAG, "onLocationChanged: location latitude = " + location.getLatitude() + ", longitude = " + location.getLongitude());
                updateSpeedByLocation(location);
                updateAltitudeByLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                MyLogUtil.d(TAG, "onStatusChanged: " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                MyLogUtil.d(TAG, "onProviderEnabled: ");
                Location location = mLocationManager.getLastKnownLocation(provider);
                updateSpeedByLocation(location);
            }

            @Override
            public void onProviderDisabled(String provider) {
                MyLogUtil.d(TAG, "onProviderDisabled: ");
            }
        };

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

        initTTS();
    }

    @Override
    public boolean isStatusBarEnabled() {
        // 使用沉浸式状态栏
        return !super.isStatusBarEnabled();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        textToSpeech.stop(); // 不管是否正在朗读TTS都被打断
        textToSpeech.shutdown(); // 关闭，释放资源
        if (getContext() != null) {
            getContext().unregisterReceiver(mBroadcastReceiver);
        }
        if (mLocationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
            } else {
                // TODO: 2019/5/9 适配Android低版本
            }
        }
    }

    @Override
    public void onPageSelect() {
        super.onPageSelect();
        boolean isAto = SPUtil.getBoolean(CommonConstance.SP_STATUS_FLASH, false);
        if (isAto) {
            mFlashLight.setText("自动");
        } else {
            mFlashLight.setText(isFlashlightOn ? "关" : "开");
        }
        boolean isTest = SPUtil.getBoolean(CommonConstance.SP_STATUS_TEST, false);
        if (isTest) {
            mLLTest.setVisibility(View.VISIBLE);
//            mTestBeyoundSpeed.setVisibility(View.VISIBLE);
        } else {
            mLLTest.setVisibility(View.GONE);
//            mTestBeyoundSpeed.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFlashLightStatusChange(FlashlightChangeEvent event) {
        if (event == null) {
            return;
        }
        isFlashlightOn = event.getStatus();
    }
    private void initTTS() {
        //实例化自带语音对象
        textToSpeech = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == textToSpeech.SUCCESS) {
                    // Toast.makeText(MainActivity.this,"成功输出语音",
                    // Toast.LENGTH_SHORT).show();
                    // Locale loc1=new Locale("us");
                    // Locale loc2=new Locale("china");

//                    textToSpeech.setPitch(1.0f);//方法用来控制音调
//                    textToSpeech.setSpeechRate(1.0f);//用来控制语速

                    //判断是否支持下面两种语言
                    int result1 = textToSpeech.setLanguage(Locale.US);
                    int result2 = textToSpeech.setLanguage(Locale.
                            SIMPLIFIED_CHINESE);
                    boolean a = (result1 == TextToSpeech.LANG_MISSING_DATA || result1 == TextToSpeech.LANG_NOT_SUPPORTED);
                    boolean b = (result2 == TextToSpeech.LANG_MISSING_DATA || result2 == TextToSpeech.LANG_NOT_SUPPORTED);

                    Log.i("zhh_tts", "US支持否？--》" + a +
                            "\nzh-CN支持否》--》" + b);

                } else {
                    toast("数据丢失或不支持");
                }

            }
        });
    }

    private void startAuto(String data) {
        // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
        textToSpeech.setPitch(0.9f);
        // 设置语速
        textToSpeech.setSpeechRate(1.0f);
        textToSpeech.speak(data,//输入中文，若不支持的设备则不会读出来
                TextToSpeech.QUEUE_FLUSH, null);

    }

    /**
     * 获取Fragment实例
     * @return
     */
    public static NewCycleFragment newInstance() {
        return new NewCycleFragment();
    }

    /**
     * 开始骑行点击事件
     */
    private void onRideStartClick() {

        mCurrentStatus = STATUS_RUNNING;
        mBtnDone.setVisibility(View.VISIBLE);
        mBtnStartOrPause.setText("暂停");
        if (modelList == null) {
            return;
        }

        mChronometer.setBase(SystemClock.elapsedRealtime() - recordTime);
        mChronometer.start();
//        mSimpleAdapter.notifyItemChanged(POSITION_TIME, FLAG_TIME_START);

        //本地检查服务是否开启
        if (!isServiceRunning) {
            //开启猎鹰轨迹上报
            startEagleReport();
        }
    }

    private void showMsgDialog() {
        String phone = SPUtil.getString(CommonConstance.SP_STATUS_PHONE);
        Context context = getContext();
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("发送短信到"+phone+"，开启开启监督骑行？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendMsg();
                        isSendMsg = true;
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        isSendMsg = false;
                    }
                }).show();
    }

    private void sendMsg() {
        String phone = SPUtil.getString(CommonConstance.SP_STATUS_PHONE);
        if (!TextUtils.isDigitsOnly(phone)) {
            toast("号码错误");
            return;
        }
        SmsManager smsManager = SmsManager.getDefault();
        String content = "【骑车帮】您的好友邀请您进行监督骑行，骑行期间您可以了解好友的实时信息。验证信息服务ID：" + serverId + " 终端号：" + terminalId;
        ArrayList<String> list = smsManager.divideMessage(content);
        for (int i = 0; i < list.size(); i++) {
            smsManager.sendTextMessage(phone, null, list.get(i), null, null);
        }
    }

    private void sendBackMsg() {
        String phone = SPUtil.getString(CommonConstance.SP_STATUS_PHONE);
        if (!TextUtils.isDigitsOnly(phone)) {
            toast("号码错误");
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        String content = "【骑车帮】您的好友已安全抵达目的地";
        ArrayList<String> list = smsManager.divideMessage(content);
        for (int i = 0; i < list.size(); i++) {
            smsManager.sendTextMessage(phone, null, list.get(i), null, null);
        }
    }


    /**
     * 暂停骑行点击事件
     */
    private void onRidePauseClick() {
        mCurrentStatus = STATUS_PAUSE;
        mBtnStartOrPause.setText("继续");
        mChronometer.stop();
        recordTime = SystemClock.elapsedRealtime() - mChronometer.getBase();
//        mSimpleAdapter.notifyItemChanged(POSITION_TIME, FLAG_TIME_PAUSE);

        //检查值得合法性：isServiceRunning应该为true，isGatherRunning应该为true
        if (!isServiceRunning
                || !isGatherRunning) {
            // TODO: 2019/5/6 恢复成开始状态
            isGatherRunning = false;
            isServiceRunning = false;
            return;
        }
        //停止采集
        mAMapTrackClient.stopGather(onTrackListener);
    }

    /**
     * 继续骑行点击事件
     */
    private void onRideContinueClick() {
        mCurrentStatus = STATUS_RUNNING;
        mBtnStartOrPause.setText("暂停");
        mChronometer.setBase(SystemClock.elapsedRealtime() - recordTime);
        mChronometer.start();
//        mSimpleAdapter.notifyItemChanged(POSITION_TIME, FLAG_TIME_CONTINUE);

        //检查值得合法性：isServiceRunning应该为true，isGatherRunning应该为false
        if (!isServiceRunning || isGatherRunning) {
            // TODO: 2019/5/6 恢复成开始状态
            isGatherRunning = false;
            isServiceRunning = false;
            return;
        }
        //继续开始收集(根据trackId判断是否为同一记录)
        long curTrackId = mAMapTrackClient.getTrackId();
        //简单的校验
        if (curTrackId == trackId) {
            mAMapTrackClient.setTrackId(trackId);
            Log.d(TAG, "onRideContinueClick: trackId = " + trackId);
            mAMapTrackClient.startGather(onTrackListener);
        }
    }

    /**
     * 结束骑行点击事件
     */
    private void onRideFinishClick() {
        mCurrentStatus = STATUS_READY;
        mBtnDone.setVisibility(View.GONE);
        mBtnStartOrPause.setText("开始");
        recordTime = 0;
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.stop();

        if (isSendMsg) {
            sendBackMsg();
        }

        //检查值得合法性：isServiceRunning应该为true，isServiceRunning应该为true
        if (!isServiceRunning || !isGatherRunning) {
            // TODO: 2019/5/6 恢复成开始状态
            isGatherRunning = false;
            isServiceRunning = false;
            return;
        }

        mAMapTrackClient.stopGather(onTrackListener);
        if (serverId != 0L && terminalId != 0) {
            mAMapTrackClient.stopTrack(new TrackParam(serverId, terminalId), onTrackListener);
        }
    }

    /**
     * 在8.0以上手机，如果app切到后台，系统会限制定位相关接口调用频率
     * 可以在启动轨迹上报服务时提供一个通知，这样Service启动时会使用该通知成为前台Service，可以避免此限制
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private Notification createNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SERVICE_RUNNING, "app service", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
            builder = new Notification.Builder(MyApplication.getContext(), CHANNEL_ID_SERVICE_RUNNING);
        } else {
            builder = new Notification.Builder(MyApplication.getContext());
        }
        Intent nfIntent = new Intent(getContext(), HomeActivity.class);
        nfIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        builder.setContentIntent(PendingIntent.getActivity(getContext(), 0, nfIntent, 0))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("猎鹰sdk运行中")
                .setContentText("猎鹰sdk运行中");
        Notification notification = builder.build();
        return notification;
    }

    /**
     * 设置定位查询条件
     * @return
     */
    private Criteria getLocationCriteria() {
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true); // 设置是否要求速度
        criteria.setCostAllowed(false); // 设置是否允许运营商收费
        criteria.setBearingRequired(false); // 设置是否需要方位信息
        criteria.setAltitudeRequired(true); // 设置是否需要海拔信息
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 设置对电源的需求
        return criteria;
    }

    /**
     * 更新时间与电量
     */
    private void updateBatteryAndTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date(System.currentTimeMillis());
        mCurTime = simpleDateFormat.format(date);
        Typeface typeface = Typeface.createFromAsset(MyApplication.getContext().getAssets(), "lcd_num.ttf");
        mTvCurTime.setTypeface(typeface);
        mTvCurTime.setText(mCurTime);

        int battery;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (mCurBattery.equals(String.valueOf(battery) + "%")) {
                return;
            }
            mCurBattery = String.valueOf(battery) + "%";
            mBattery.setTypeface(typeface);
            mBattery.setText(mCurBattery);
        }
    }

    /**
     * 根据位置获取海拔信息
     * @param location
     */
    private void updateAltitudeByLocation(Location location) {
//        if (location == null) {
//            return;
//        }
//        double accAltitude = location.getAltitude();
//        MyLogUtil.d(TAG, "updateAltitudeByLocation: " + accAltitude);
//        SimpleModel model = modelList.get(POSITION_ALTITUDE);
//        if (model instanceof  RunningDataModel) {
//            ((RunningDataModel) model).mData = String.valueOf(Math.round(accAltitude));
//        }
//        mSimpleAdapter.notifyItemChanged(POSITION_ALTITUDE, FLAG_UPDATE_ALTITUDE);
    }

    /**
     * 根据位置获取速度
     * @param location
     */
    private void updateSpeedByLocation(Location location) {
        if (location == null) {
            return;
        }
        float speed = location.getSpeed();
        String maxSpeed = SPUtil.getString(CommonConstance.SP_STATUS_SPEED_MAX);
        float maxSpeedFloat = defaultSpeed;
        if (TextUtils.isDigitsOnly(maxSpeed)) {
            maxSpeedFloat = Float.valueOf(maxSpeed);
        }
        float speedPerSec = maxSpeedFloat / 3.6f;
        Log.d(TAG, "updateSpeedByLocation: " + speedPerSec);
        if (speed >= speedPerSec) {
            boolean isSpeedOn = SPUtil.getBoolean(CommonConstance.SP_STATUS_SPEED, true);
            if (isSpeedOn && !isCountDonw) {
                startAuto("骑车帮提醒您：请减速慢行，道路千万条，安全第一条。行车不规范，亲人两行泪");
                isCountDonw = true;
                curSec = 30;
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        curSec--;
                        if (curSec == 0) {
                            isCountDonw = false;
                            cancel();
                        }
                    }
                };
                timer.schedule(task, 0, 1000);
            }
        }
        MyLogUtil.d(TAG, "updateSpeedByLocation: speed = " + speed);
        mTvDebug.setText("speed: " + speed);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            float speedAccuracyMetersPerSecond = location.getSpeedAccuracyMetersPerSecond();
            MyLogUtil.d(TAG, "updateSpeedByLocation: speedAccuracyMetersPerSecond = " + speedAccuracyMetersPerSecond);
        }
        mDashboardView.updateSpeed(speed);
    }

    /**
     * 更新距离查询
     * @param distance
     */
//    private void updateDistance(int distance) {
//        String distanceString = String.valueOf(distance);
//        SimpleModel modelDistance = modelList.get(POSITION_KM);
//        if (modelDistance instanceof  RunningDataModel) {
//            String curDistance = ((RunningDataModel) modelDistance).mData;
//            if (distanceString.equals(curDistance)) {
//                return;
//            }
//            ((RunningDataModel) modelDistance).mData = String.valueOf(distance);
//        }
//        mSimpleAdapter.notifyDataSetChanged();
//    }

    /**
     * 开启猎鹰上报服务
     */
    private void startEagleReport() {
        //远端检查服务是否开启
        EagleApiService service = getEagleCallService();
        service.listService(CommonConstance.KEY_EAGLE).enqueue(new Callback<CommonEagleNetModel<ServerListModel>>() {
            @Override
            public void onResponse(Call<CommonEagleNetModel<ServerListModel>> call, Response<CommonEagleNetModel<ServerListModel>> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                CommonEagleNetModel<ServerListModel> model = response.body();
                if (model == null) {
                    return;
                }
                ServerListModel data = model.data;
                if (data == null) {
                    return;
                }
                List<FullServerModel> results = data.results;
                if (results == null
                        || results.isEmpty()) {
                    return;
                }
                String userId = getUserId();
                if (TextUtils.isEmpty(userId)) {
                    toast("user_id 为空");
                    return;
                }
                FullServerModel targetServerModel = null;
                String serviceName = CommonConstance.SERVICE_HEAD+ userId;
                for (FullServerModel serverModel : results) {
                    if (serviceName.equals(serverModel.name)) {
                        targetServerModel = serverModel;
                        break;
                    }
                }
                //targetServerModel不为空表示找到创建的Service
                if (targetServerModel != null) {
                    //service已经创建
                    serverId = targetServerModel.sid;
                    SPUtil.putLong(CommonConstance.SP_SERVER_ID, serverId);
                    startTrack();
                } else {
                    //Service没有创建
                    createEagleService();
                }
            }

            @Override
            public void onFailure(Call<CommonEagleNetModel<ServerListModel>> call, Throwable t) {
                toast(t.toString());
            }
        });
    }

    /**
     * 创建猎鹰服务
     */
    private void createEagleService() {
        String userId = getUserId();
        if (TextUtils.isEmpty(userId)) {
            toast("user_id 为空");
            return;
        }
        String name = CommonConstance.SERVICE_HEAD + userId;
        String des = "record_ride";
        EagleApiService service = getEagleCallService();
        service.addService(CommonConstance.KEY_EAGLE, name, des)
                .enqueue(new Callback<CommonEagleNetModel<SimpleServerModel>>() {
                    @Override
                    public void onResponse(Call<CommonEagleNetModel<SimpleServerModel>> call, Response<CommonEagleNetModel<SimpleServerModel>> response) {
                        if (!response.isSuccessful()) {
                            return;
                        }
                        CommonEagleNetModel<SimpleServerModel>  model = response.body();
                        if (model == null) {
                            return;
                        }

                        SimpleServerModel data = model.data;
                        if (data == null) {
                            return;
                        }
                        serverId = data.sid;
                        SPUtil.putLong(CommonConstance.SP_SERVER_ID, serverId);
                        startTrack();
                    }

                    @Override
                    public void onFailure(Call<CommonEagleNetModel<SimpleServerModel>> call, Throwable t) {
                        toast(t.toString());
                    }
                });
    }

    /**
     * 定位点集收集
     */
    private void startTrack() {
        // 先根据Terminal名称查询Terminal ID，如果Terminal还不存在，就尝试创建，拿到Terminal ID后，
        // 用Terminal ID开启轨迹服务
        mAMapTrackClient.queryTerminal(new QueryTerminalRequest(serverId, CommonConstance.TERMINAL_HEAD), new SimpleOnTrackListener() {
            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {
                if (queryTerminalResponse.isSuccess()) {
                    if (queryTerminalResponse.isTerminalExist()) {
                        // 当前终端已经创建过，直接使用查询到的terminal id
                        terminalId = queryTerminalResponse.getTid();
                        SPUtil.putLong(CommonConstance.SP_TERMINAL_ID, terminalId);
                        showMsgDialog();
                        addTrack();
                    } else {
                        // 当前终端是新终端，还未创建过，创建该终端并使用新生成的terminal id
                        mAMapTrackClient.addTerminal(new AddTerminalRequest(CommonConstance.TERMINAL_HEAD, serverId), new SimpleOnTrackListener() {
                            @Override
                            public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {
                                if (addTerminalResponse.isSuccess()) {
                                    terminalId = addTerminalResponse.getTid();
                                    SPUtil.putLong(CommonConstance.SP_TERMINAL_ID, terminalId);
                                    showMsgDialog();
                                    addTrack();
                                } else {
                                    Toast.makeText(getContext(), "网络请求失败，" + addTerminalResponse.getErrorMsg(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(getContext(), "网络请求失败，" + queryTerminalResponse.getErrorMsg(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 添加新轨迹
     */
    private void addTrack() {
        mAMapTrackClient.addTrack(new AddTrackRequest(serverId, terminalId), new SimpleOnTrackListener() {
            @Override
            public void onAddTrackCallback(AddTrackResponse addTrackResponse) {
                if (addTrackResponse.isSuccess()) {
                    // trackId需要在启动服务后设置才能生效，因此这里不设置，而是在startGather之前设置了track id
                    trackId = addTrackResponse.getTrid();
                    TrackParam trackParam = new TrackParam(serverId, terminalId);
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        trackParam.setNotification(createNotification());
                    }
                    mAMapTrackClient.startTrack(trackParam, onTrackListener);
                } else {
                    Toast.makeText(getContext(), "网络请求失败，" + addTrackResponse.getErrorMsg(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * 获取用户id
     * @return
     */
    private String getUserId() {
        return "user_4";
    }

    /**
     * 获取猎鹰请求服务
     * @return
     */
    private EagleApiService getEagleCallService() {
        RetrofitWrapper wrapper = RetrofitWrapper.getInstance(CommonConstance.EAGLE_URL);
        return wrapper.getEagleCall();
    }

    private void queryClick() {
        OnTrackListener trackListener = new OnTrackListener(){

            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {

            }

            @Override
            public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {

            }

            @Override
            public void onDistanceCallback(DistanceResponse distanceResponse) {
                if (distanceResponse.isSuccess()) {
                    double meters = distanceResponse.getDistance();
                    // 行驶里程查询成功，行驶了meters米
                    Log.d(TAG, "onDistanceCallback: " + meters);
//                    updateDistance((int) meters);
                } else {
                    // 行驶里程查询失败
                }
            }

            @Override
            public void onLatestPointCallback(LatestPointResponse latestPointResponse) {
                if (latestPointResponse.isSuccess()) {
                    Point point = latestPointResponse.getLatestPoint().getPoint();
                    // 查询实时位置成功，point为实时位置信息
                } else {
                    // 查询实时位置失败
                }
            }

            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {

            }

            @Override
            public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {
                if (queryTrackResponse.isSuccess()) {
                    List<Track> tracks =  queryTrackResponse.getTracks();
                    if (tracks != null) {
                        Log.d(TAG, "onQueryTrackCallback: size = " + tracks.size());
                        for (int i = 0; i < tracks.size(); i++) {
                            Track curTrack = tracks.get(i);
                            Log.d(TAG, "onQueryTrackCallback: " + i + ": distance = " + curTrack.getDistance() + " id =" +
                                    "" + curTrack.getTrid());
                        }
                    }
                    // 查询成功，tracks包含所有轨迹及相关轨迹点信息
                } else {
                    // 查询失败
                }
            }

            @Override
            public void onAddTrackCallback(AddTrackResponse addTrackResponse) {

            }

            @Override
            public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {

            }
        };
        long cur = System.currentTimeMillis();
        DistanceRequest distanceRequest = new DistanceRequest(serverId, terminalId, cur - 1 * 60 * 60 * 1000, cur, -1);
        mAMapTrackClient.queryDistance(distanceRequest, trackListener);
    }

    /**
     * 测试方法
     * start查新测试
     */
    @OnClick(R.id.query)
    void onQuery() {
        OnTrackListener trackListener = new OnTrackListener(){

            @Override
            public void onQueryTerminalCallback(QueryTerminalResponse queryTerminalResponse) {

            }

            @Override
            public void onCreateTerminalCallback(AddTerminalResponse addTerminalResponse) {

            }

            @Override
            public void onDistanceCallback(DistanceResponse distanceResponse) {
                if (distanceResponse.isSuccess()) {
                    double meters = distanceResponse.getDistance();
                    // 行驶里程查询成功，行驶了meters米
                    Log.d(TAG, "onDistanceCallback: " + meters);
//                    updateDistance((int) meters);
                } else {
                    // 行驶里程查询失败
                }
            }

            @Override
            public void onLatestPointCallback(LatestPointResponse latestPointResponse) {
                if (latestPointResponse.isSuccess()) {
                    Point point = latestPointResponse.getLatestPoint().getPoint();
                    // 查询实时位置成功，point为实时位置信息
                    if (point == null) {
                        return;
                    }
                    toast("维度: " + point.getLat() + "经度：" + point.getLng());
                } else {
                    // 查询实时位置失败
                }
            }

            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {

            }

            @Override
            public void onQueryTrackCallback(QueryTrackResponse queryTrackResponse) {

            }

            @Override
            public void onAddTrackCallback(AddTrackResponse addTrackResponse) {

            }

            @Override
            public void onParamErrorCallback(ParamErrorResponse paramErrorResponse) {

            }
        };

        mAMapTrackClient.queryLatestPoint(new LatestPointRequest(serverId, terminalId), trackListener);
    }
}
