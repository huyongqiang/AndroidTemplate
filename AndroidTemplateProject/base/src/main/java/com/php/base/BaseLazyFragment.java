package com.php.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.util.Random;

/**
 *    author : xxx
 *    xxx
 *    time   : 2018/10/18
 *    desc   : Fragment 懒加载基类
 *    illustration:
 *    ①有时候我们看到别人写的Fragment的时候，会先思考这个Fragment到底是哪个Activity里面的，由此我们可以用泛型，可以更直观地看到这个Fragment是属于哪个Activity，Fragment用于多个不同的Activity的时候，这个泛型也可以不用指定。
 *    ②特色点：在Fragment加了getBindingActivity方法，可自动强转至具体类型的Activity，无需手动进行强转
 *    ③很多人知道Fragment，但是对懒加载的Fragment了解不是很深刻，何为懒加载，举个例子，单例设计模式中也有一个懒汉式，只有调用的时候才会初始化，懒加载也是基于这种思想，看见的时候才加载网络数据，并且只加载一次，我都知道调用replace方法会导致Fragment再走一次生命周期，这就导致了布局被重新创建和接口数据被重复请求，对用户的体验和应用的性能是极其不好的，我们可以将Fragment的Layout进行缓存，在Fragment重复调用生命周期的时候进行判断，在onCreateView方法中将之前创建过的Layout返回回去，而不是使用布局填充器重复创建
 *    即：懒加载Fragment的布局只创建了一次，界面数据也只初始化了一次
 *    谷歌官方只提供了Fragment类，但是并没有实现懒加载机制，此处通过对Fragment的二次封装来实现这一机制
 */
public abstract class BaseLazyFragment<A extends BaseActivity> extends Fragment {

    // Activity对象
    public A mActivity;
    // 根布局
    private View mRootView;
    // 是否进行过懒加载
    private boolean isLazyLoad;
    // Fragment 是否可见
    private boolean isFragmentVisible;
    // 是否是 replace Fragment 的形式
    private boolean isReplaceFragment;

    /**
     * 获得全局的，防止使用getActivity()为空
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (A) requireActivity();
    }

    /**
     * 获取绑定的Activity，防止出现 getActivity() 为空
     */
    public A getBindingActivity() {
        return mActivity;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (mRootView == null && getLayoutId() > 0) {
            mRootView = inflater.inflate(getLayoutId(), null);
        }

        ViewGroup parent = (ViewGroup) mRootView.getParent();
        if (parent != null) {
            parent.removeView(mRootView);
        }

        return mRootView;
    }

    @Override
    public View getView() {
        return mRootView;
    }

    /**
     * 是否进行了懒加载
     */
    protected boolean isLazyLoad() {
        return isLazyLoad;
    }

    /**
     * 当前 Fragment 是否可见
     */
    public boolean isFragmentVisible() {
        return isFragmentVisible;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (isReplaceFragment) {
            if (isFragmentVisible) {
                initLazyLoad();
            }
        }else {
            initLazyLoad();
        }
    }

    // replace Fragment时使用，ViewPager 切换时会回调此方法
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isReplaceFragment = true;
        this.isFragmentVisible = isVisibleToUser;
        if (isVisibleToUser && mRootView != null) {
            if (!isLazyLoad) {
                initLazyLoad();
            }else {
                // 从不可见到可见
                onRestart();
            }
        }
    }

    /**
     * 初始化懒加载
     */
    protected void initLazyLoad() {
        if (!isLazyLoad) {
            isLazyLoad = true;
            initFragment();
        }
    }

    /**
     * 从可见的状态变成不可见状态，再从不可见状态变成可见状态时回调
     */
    protected void onRestart() {}

    @Override
    public void onDetach() {
        super.onDetach();
        // 解决java.lang.IllegalStateException: Activity has been destroyed 的错误
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initFragment() {
        initView();
        initData();
    }

    //引入布局
    protected abstract int getLayoutId();

    //标题栏id
    protected abstract int getTitleBarId();

    //初始化控件
    protected abstract void initView();

    //初始化数据
    protected abstract void initData();

    /**
     * 根据资源 id 获取一个 View 对象
     */
    protected <V extends View> V findViewById(@IdRes int id) {
        return mRootView.findViewById(id);
    }

    protected <V extends View> V findActivityViewById(@IdRes int id) {
        return mActivity.findViewById(id);
    }


    /**
     * startActivity 方法优化
     */

    public void startActivity(Class<? extends Activity> cls) {
        startActivity(new Intent(mActivity, cls));
    }

    public void startActivityFinish(Class<? extends Activity> cls) {
        startActivityFinish(new Intent(mActivity, cls));
    }

    public void startActivityFinish(Intent intent) {
        startActivity(intent);
        finish();
    }

    /**
     * startActivityForResult 方法优化
     */

    private BaseActivity.ActivityCallback mActivityCallback;
    private int mActivityRequestCode;

    public void startActivityForResult(Class<? extends Activity> cls, BaseActivity.ActivityCallback callback) {
        startActivityForResult(new Intent(mActivity, cls), null, callback);
    }

    public void startActivityForResult(Intent intent, BaseActivity.ActivityCallback callback) {
        startActivityForResult(intent, null, callback);
    }

    public void startActivityForResult(Intent intent, Bundle options, BaseActivity.ActivityCallback callback) {
        if (mActivityCallback == null) {
            mActivityCallback = callback;
            // 随机生成请求码，这个请求码在 0 - 255 之间
            mActivityRequestCode = new Random().nextInt(255);
            startActivityForResult(intent, mActivityRequestCode, options);
        }else {
            // 回调还没有结束，所以不能再次调用此方法，这个方法只适合一对一回调，其他需求请使用原生的方法实现
            throw new IllegalArgumentException("Error, The callback is not over yet");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (mActivityCallback != null && mActivityRequestCode == requestCode) {
            mActivityCallback.onActivityResult(resultCode, data);
            mActivityCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 销毁当前 Fragment 所在的 Activity
     */
    public void finish() {
        mActivity.finish();
        mActivity = null;
    }

    /**
     * 获取系统服务
     */
    @SuppressWarnings("unchecked")
    public <S extends Object> S getSystemService(@NonNull String name) {
        return (S) mActivity.getSystemService(name);
    }

    /**
     * Fragment返回键被按下时回调
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //默认不拦截按键事件，传递给Activity
        return false;
    }
}