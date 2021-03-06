package com.nexuslink.alphrye.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.nexuslink.alphrye.common.MyActivity;
import com.nexuslink.alphrye.cyctastic.R;
import com.nexuslink.alphrye.helper.EditTextInputHelper;
import com.hjq.toast.ToastUtils;
import com.nexuslink.alphrye.model.ProfileModel;
import com.nexuslink.alphrye.net.wrapper.RetrofitWrapper;

import butterknife.BindView;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : 登录界面
 */
public class LoginActivity extends MyActivity
        implements View.OnClickListener {

    @BindView(R.id.et_login_phone)
    EditText mPhoneView;
    @BindView(R.id.et_login_password)
    EditText mPasswordView;
    @BindView(R.id.btn_login_commit)
    Button mCommitView;
    @BindView(R.id.tv_login_forget)
    TextView mLoginForgetView;
    @BindView(R.id.tv_register)
    TextView mTvRegister;

    private EditTextInputHelper mEditTextInputHelper;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    protected int getTitleBarId() {
        return R.id.tb_login_title;
    }

    @Override
    protected void initView() {
        mCommitView.setOnClickListener(this);
        mLoginForgetView.setOnClickListener(this);
        mTvRegister.setOnClickListener(this);
        mEditTextInputHelper = new EditTextInputHelper(mCommitView);
        mEditTextInputHelper.addViews(mPhoneView, mPasswordView);
    }

    @Override
    protected void initData() {

    }

    @Override
    public void onRightClick(View v) {
        // 跳转到注册界面
        startActivity(RegisterActivity.class);

//        startActivityForResult(new Intent(this, RegisterActivity.class), new ActivityCallback() {
//
//            @Override
//            public void onActivityResult(int resultCode, @Nullable Intent data) {
//                toast(String.valueOf(resultCode));
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        mEditTextInputHelper.removeViews();
        super.onDestroy();
    }

    @Override
    public boolean isSupportSwipeBack() {
        //不使用侧滑功能
        return !super.isSupportSwipeBack();
    }

    /**
     * {@link View.OnClickListener}
     */
    @Override
    public void onClick(View v) {
        if (v == mCommitView) {
            if (mPhoneView.getText().toString().length() != 11) {
                ToastUtils.show(getResources().getString(R.string.phone_input_error));
            }
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage("登录中...");
            dialog.show();
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Intent intent = new Intent(getContext(), HomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 1000);
        } else if (v == mLoginForgetView) {
            startActivity(HomeActivity.class);
        } else if (v == mTvRegister) {
            startActivity(RegisterActivity.class);
            //发起登录请求
//            RetrofitWrapper wrapper = RetrofitWrapper.getInstance();
//            wrapper.enqueue(wrapper.getCommonCall().login(), new RetrofitWrapper.CommonCallBack<ProfileModel>() {
//                @Override
//                public void onSuccess(ProfileModel response) {
//                    if (response == null) {
//                        //fail
//                        return;
//                    }
//                    startActivity(HomeActivity.class);
//                    //保存数据到Sp
//                }
//
//                @Override
//                public void onFail(String errorTips) {
//
//                }
//            });
        }
    }
}