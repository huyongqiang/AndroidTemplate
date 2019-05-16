package com.php.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatDialog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 *    author : xxx
 *    xxx
 *    time   : 2018/11/24
 *    desc   : Dialog 基类
 *    illustration:
 *         代码的摆放和顺序都是经过特殊处理的，摆放是通过代码类型，监听器的代码放一起，设置监听的方法放一起，添加监听的方法放一起;
 *         顺序是通过代码的执行顺序进行摆放的，比如Dialog的三个监听器，最先回调是显示监听，再者是取消监听，最后才是销毁监听
*    problem  illustratiion:
 *    使用DialogFragment之后出现的一个问题，就是原先给Dialog设置的取消监听和销毁监听失效了，而单单使用Dialog却没有任何问题
*      原来DialogFragment的生命周期方法onActivityCreated重新给Dialog设置了取消监听和销毁监听，所以才会导致我们给Dialog设置的监听无效
*       DialogFragment其实占用Dialog的监听并非无用，而是通过监听Dialog的销毁从而将自己从Activity中剔除掉
*      那么我们需要考虑既能不破坏DialogFragment的监听，也不影响我们对Dialog的监听，怎么做呢？
*   本处觉得还不错的方法是修改Dialog原有的监听规则：
*       观察者设计模式分为两种，一种是一对一（一个被观察者对应着一个观察者），一种一对多，而Dialog的设置监听正是采用了一对一的模式，这样会导致一个问题，监听器对象只能有一个，而一对多正好解决了这个问题
*       那么为什么可以用一对多这种模式呢？什么情况下就不能用呢？如果这个监听器采用了责任链的设计模式，则我们不能用一对多，只能使用一对一，因为责任链模式一般都会通过监听器的方法返回值来决定是否回调下一个监听器对象，
*       如果上一个监听器拦截了，那么下个监听器就接收不到了，这个时候就必须要使用一对一。而刚好我们刚刚讲的Dialog的两个监听器的方法都不需要返回值，所以我们可以对它们进行改造
 */
public class BaseDialog extends AppCompatDialog implements
        DialogInterface.OnShowListener,
        DialogInterface.OnCancelListener,
        DialogInterface.OnDismissListener {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private List<BaseDialog.OnShowListener> mOnShowListeners;
    private List<BaseDialog.OnCancelListener> mOnCancelListeners;
    private List<BaseDialog.OnDismissListener> mOnDismissListeners;

    public BaseDialog(Context context) {
        this(context, R.style.BaseDialogStyle);
    }

    public BaseDialog(Context context, int themeResId) {
        super(context, themeResId > 0 ? themeResId : R.style.BaseDialogStyle);
    }

    /**
     * 设置一个显示监听器
     *
     * @param listener       监听器对象
     * @deprecated          请使用 {@link #addOnShowListener(BaseDialog.OnShowListener)}}
     */
    @Deprecated
    @Override
    public void setOnShowListener(@Nullable DialogInterface.OnShowListener listener) {
        addOnShowListener(new ShowListenerWrapper(listener));
    }

    /**
     * 设置一个取消监听器
     *
     * @param listener       监听器对象
     * @deprecated          请使用 {@link #addOnCancelListener(BaseDialog.OnCancelListener)}
     * illustration :
     * 使用了外观设计模式，可以在不改变父类方法的参数类型前提下，使用包装类对原有的接口进行回调，在不破坏方法的参数类型情况下对类型进行转换
     *
     */
    @Deprecated
    @Override
    public void setOnCancelListener(@Nullable DialogInterface.OnCancelListener listener) {
        addOnCancelListener(new CancelListenerWrapper(listener));
    }

    /**
     * 设置一个销毁监听器
     *
     * @param listener       监听器对象
     * @deprecated          请使用 {@link #addOnDismissListener(BaseDialog.OnDismissListener)}
     */
    @Deprecated
    @Override
    public void setOnDismissListener(@Nullable DialogInterface.OnDismissListener listener) {
        addOnDismissListener(new DismissListenerWrapper(listener));
    }

    /**
     * 添加一个取消监听器
     *
     * @param listener      监听器对象
     *
     */
    public void addOnShowListener(@Nullable BaseDialog.OnShowListener listener) {
        if (mOnShowListeners == null) {
            mOnShowListeners = new ArrayList<>();
            super.setOnShowListener(this);
        }
        mOnShowListeners.add(listener);
    }

    /**
     * 添加一个取消监听器
     *
     * @param listener      监听器对象
     */
    public void addOnCancelListener(@Nullable BaseDialog.OnCancelListener listener) {
        if (mOnCancelListeners == null) {
            mOnCancelListeners = new ArrayList<>();
            super.setOnCancelListener(this);
        }
        mOnCancelListeners.add(listener);
    }

    /**
     * 添加一个销毁监听器
     *
     * @param listener      监听器对象
     */
    public void addOnDismissListener(@Nullable BaseDialog.OnDismissListener listener) {
        if (mOnDismissListeners == null) {
            mOnDismissListeners = new ArrayList<>();
            super.setOnDismissListener(this);
        }
        mOnDismissListeners.add(listener);
    }

    /**
     * 设置显示监听器集合
     */
    private void setOnShowListeners(@Nullable List<BaseDialog.OnShowListener> listeners) {
        super.setOnShowListener(this);
        mOnShowListeners = listeners;
    }

    /**
     * 设置取消监听器集合
     */
    private void setOnCancelListeners(@Nullable List<BaseDialog.OnCancelListener> listeners) {
        super.setOnCancelListener(this);
        mOnCancelListeners = listeners;
    }

    /**
     * 设置销毁监听器集合
     */
    private void setOnDismissListeners(@Nullable List<BaseDialog.OnDismissListener> listeners) {
        super.setOnDismissListener(this);
        mOnDismissListeners = listeners;
    }

    /**
     * {@link DialogInterface.OnShowListener}
     *
     * illustration:
     *      Dialog的监听器从观察者一对一改造成了一对多(其他监听器同理)，在原先的一对一的方法上直接回调一对多的方法，
     *      然后在回调方法的里面再去调所有的监听对象，这样我们再不用担心DialogFragment占用Dialog原有的监听器了，因为它现在支持设置多个监听器对象
     */
    @Override
    public void onShow(DialogInterface dialog) {
        if (mOnShowListeners != null) {
            for (BaseDialog.OnShowListener listener : mOnShowListeners) {
                listener.onShow(this);
            }
        }
    }

    /**
     * {@link DialogInterface.OnCancelListener}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        if (mOnCancelListeners != null) {
            for (BaseDialog.OnCancelListener listener : mOnCancelListeners) {
                listener.onCancel(this);
            }
        }
    }

    /**
     * {@link DialogInterface.OnDismissListener}
     */
    @Override
    public void onDismiss(DialogInterface dialog) {

        // 移除和这个 Dialog 相关的消息回调
        HANDLER.removeCallbacksAndMessages(this);

        if (mOnDismissListeners != null) {
            for (BaseDialog.OnDismissListener listener : mOnDismissListeners) {
                listener.onDismiss(this);
            }
        }
    }

    /**
     * 延迟执行
     */
    public final boolean post(Runnable r) {
        return postDelayed(r, 0);
    }

    /**
     * 延迟一段时间执行
     */
    public final boolean postDelayed(Runnable r, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return postAtTime(r, SystemClock.uptimeMillis() + delayMillis);
    }

    /**
     * 在指定的时间执行
     */
    public final boolean postAtTime(Runnable r, long uptimeMillis) {
        return HANDLER.postAtTime(r, this, uptimeMillis);
    }

    /**
     * Dialog 动画样式
     */
    public static final class AnimStyle {

        // 默认动画效果
        static final int DEFAULT = R.style.ScaleAnimStyle;

        // 缩放动画
        public static final int SCALE = R.style.ScaleAnimStyle;

        // IOS 动画
        public static final int IOS = R.style.IOSAnimStyle;

        // 吐司动画
        public static final int TOAST = android.R.style.Animation_Toast;

        // 顶部弹出动画
        public static final int TOP = R.style.TopAnimStyle;

        // 底部弹出动画
        public static final int BOTTOM = R.style.BottomAnimStyle;

        // 左边弹出动画
        public static final int LEFT = R.style.LeftAnimStyle;

        // 右边弹出动画
        public static final int RIGHT = R.style.RightAnimStyle;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<B extends Builder> {

        protected static final int MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT;
        protected static final int WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT;

        private BaseDialog mDialog;

        // Context 对象
        private Context mContext;

        // Dialog 布局
        private View mContentView;

        // Dialog Show 监听
        private List<BaseDialog.OnShowListener> mOnShowListeners;
        // Dialog Cancel 监听
        private List<BaseDialog.OnCancelListener> mOnCancelListeners;
        // Dialog Dismiss 监听
        private List<BaseDialog.OnDismissListener> mOnDismissListeners;
        // Dialog Key 监听
        private OnKeyListener mOnKeyListener;

        // 点击空白是否能够取消  默认点击阴影可以取消
        private boolean mCancelable = true;

        private SparseArray<CharSequence> mTextArray = new SparseArray<>();
        private SparseIntArray mVisibilityArray = new SparseIntArray();
        private SparseArray<Drawable> mBackgroundArray = new SparseArray<>();
        private SparseArray<Drawable> mImageArray = new SparseArray<>();
        private SparseArray<BaseDialog.OnClickListener> mClickArray = new SparseArray<>();

        // 主题
        private int mThemeResId = -1;
        // 动画
        private int mAnimations = -1;
        // 位置
        private int mGravity = Gravity.CENTER;
        // 宽度和高度
        private int mWidth = WRAP_CONTENT;
        private int mHeight = WRAP_CONTENT;
        // 垂直和水平边距
        private int mVerticalMargin;
        private int mHorizontalMargin;

        public Builder(Context context) {
            mContext = context;
        }

        /**
         * 延迟执行，一定要在创建了Dialog之后调用（供子类调用）
         */
        protected final boolean post(Runnable r) {
            return mDialog.post(r);
        }

        /**
         * 延迟一段时间执行，一定要在创建了Dialog之后调用（仅供子类调用）
         */
        protected final boolean postDelayed(Runnable r, long delayMillis) {
            return mDialog.postDelayed(r, delayMillis);
        }

        /**
         * 在指定的时间执行，一定要在创建了Dialog之后调用（仅供子类调用）
         */
        protected final boolean postAtTime(Runnable r, long uptimeMillis) {
            return mDialog.postAtTime(r, uptimeMillis);
        }

        /**
         * 是否设置了取消（仅供子类调用）
         */
        protected boolean isCancelable() {
            return mCancelable;
        }

        /**
         * 获取上下文对象（仅供子类调用）
         */
        protected Context getContext() {
            return mContext;
        }

        /**
         * 获取 Dialog 重心（仅供子类调用）
         */
        protected int getGravity() {
            return mGravity;
        }

        /**
         * 获取资源对象（仅供子类调用）
         */
        protected Resources getResources() {
            return mContext.getResources();
        }

        /**
         * 根据 id 获取一个文本（仅供子类调用）
         */
        protected CharSequence getText(@StringRes int resId) {
            return mContext.getText(resId);
        }

        /**
         * 根据 id 获取一个 String（仅供子类调用）
         */
        protected String getString(@StringRes int resId) {
            return mContext.getString(resId);
        }

        /**
         * 根据 id 获取一个颜色（仅供子类调用）
         */
        protected int getColor(@ColorRes int id) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return mContext.getColor(id);
            }else {
                return mContext.getResources().getColor(id);
            }
        }

        /**
         * 根据 id 获取一个 Drawable（仅供子类调用）
         */
        protected Drawable getDrawable(@DrawableRes int id) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return mContext.getDrawable(id);
            }else {
                return mContext.getResources().getDrawable(id);
            }
        }

        /**
         * 根据 id 查找 View（仅供子类调用）
         */
        protected <T extends View> T findViewById(@IdRes int id) {
            return mContentView.findViewById(id);
        }

        /**
         * 获取当前 Dialog 对象（仅供子类调用）
         */
        protected BaseDialog getDialog() {
            return mDialog;
        }

        /**
         * 销毁当前 Dialog（仅供子类调用）
         */
        protected void dismiss() {
            mDialog.dismiss();
        }

        /**
         * 设置主题 id
         */
        public B setThemeStyle(@StyleRes int themeResId) {
            mThemeResId = themeResId;
            return (B) this;
        }

        /**
         * 设置布局
         */
        public B setContentView(@LayoutRes int layoutId) {
            return setContentView(LayoutInflater.from(mContext).inflate(layoutId, null));
        }
        public B setContentView(@NonNull View view) {
            mContentView = view;
            return (B) this;
        }

        /**
         * 设置重心位置
         */
        public B setGravity(int gravity) {
            // 适配 Android 4.2 新特性，布局反方向（开发者选项 - 强制使用从右到左的布局方向）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                gravity = Gravity.getAbsoluteGravity(gravity, mContext.getResources().getConfiguration().getLayoutDirection());
            }
            mGravity = gravity;
            if (mAnimations == -1) {
                switch (mGravity) {
                    case Gravity.TOP:
                        mAnimations = AnimStyle.TOP;
                        break;
                    case Gravity.BOTTOM:
                        mAnimations = AnimStyle.BOTTOM;
                        break;
                    case Gravity.LEFT:
                        mAnimations = AnimStyle.LEFT;
                        break;
                    case Gravity.RIGHT:
                        mAnimations = AnimStyle.RIGHT;
                        break;
                }
            }
            return (B) this;
        }

        /**
         * 设置宽度
         */
        public B setWidth(int width) {
            mWidth = width;
            return (B) this;
        }

        /**
         * 设置高度
         */
        public B setHeight(int height) {
            mHeight = height;
            return (B) this;
        }

        /**
         * 是否可以取消
         */
        public B setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return (B) this;
        }

        /**
         * 设置动画，已经封装好几种样式，具体可见{@link AnimStyle}类
         */
        public B setAnimStyle(@StyleRes int resId) {
            mAnimations = resId;
            return (B) this;
        }

        /**
         * 设置垂直间距
         */
        public B setVerticalMargin(int margin) {
            mVerticalMargin = margin;
            return (B) this;
        }

        /**
         * 设置水平间距
         */
        public B setHorizontalMargin(int margin) {
            mHorizontalMargin = margin;
            return (B) this;
        }

        /**
         * 添加显示监听
         */
        public B addOnShowListener(@NonNull BaseDialog.OnShowListener listener) {
            if (mOnShowListeners == null) {
                mOnShowListeners = new ArrayList<>();
            }
            mOnShowListeners.add(listener);
            return (B) this;
        }

        /**
         * 添加取消监听
         */
        public B addOnCancelListener(@NonNull BaseDialog.OnCancelListener listener) {
            if (mOnCancelListeners == null) {
                mOnCancelListeners = new ArrayList<>();
            }
            mOnCancelListeners.add(listener);
            return (B) this;
        }

        /**
         * 添加销毁监听
         */
        public B addOnDismissListener(@NonNull BaseDialog.OnDismissListener listener) {
            if (mOnDismissListeners == null) {
                mOnDismissListeners = new ArrayList<>();
            }
            mOnDismissListeners.add(listener);
            return (B) this;
        }

        /**
         * 设置按键监听
         */
        public B setOnKeyListener(@NonNull OnKeyListener onKeyListener) {
            mOnKeyListener = onKeyListener;
            return (B) this;
        }

        /**
         * 设置文本
         */
        public B setText(@IdRes int id, @StringRes int resId) {
            return setText(id, mContext.getResources().getString(resId));
        }
        public B setText(@IdRes int id, CharSequence text) {
            mTextArray.put(id, text);
            return (B) this;
        }

        /**
         * 设置可见状态
         */
        public B setVisibility(@IdRes int id, int visibility) {
            mVisibilityArray.put(id, visibility);
            return (B) this;
        }

        /**
         * 设置背景
         */
        public B setBackground(@IdRes int id, @DrawableRes int resId) {
            return setBackground(id, mContext.getResources().getDrawable(resId));
        }
        public B setBackground(@IdRes int id, Drawable drawable) {
            mBackgroundArray.put(id, drawable);
            return (B) this;
        }

        /**
         * 设置图片
         */
        public B setImageDrawable(@IdRes int id, @DrawableRes int resId) {
            return setBackground(id, mContext.getResources().getDrawable(resId));
        }
        public B setImageDrawable(@IdRes int id, Drawable drawable) {
            mImageArray.put(id, drawable);
            return (B) this;
        }

        /**
         * 设置点击事件
         */
        public B setOnClickListener(@IdRes int id, @NonNull BaseDialog.OnClickListener listener) {
            mClickArray.put(id, listener);
            return (B) this;
        }

        /**
         * 创建
         */
        public BaseDialog create() {

            // 判断布局是否为空
            if (mContentView == null) {
                throw new IllegalArgumentException("Dialog layout cannot be empty");
            }

            ViewGroup.LayoutParams layoutParams = mContentView.getLayoutParams();
            if (layoutParams != null) {

                if (mWidth == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    mWidth = layoutParams.width;
                }
                if (mHeight == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    mHeight = layoutParams.height;
                }
            }

//            // 判断有没有设置主题
//            if (mThemeResId == -1) {
//                mDialog = new BaseDialog(mContext);
//            } else {
//                mDialog = new BaseDialog(mContext, mThemeResId);
//            }

            mDialog = createDialog(mContext, mThemeResId);

            mDialog.setContentView(mContentView);

            mDialog.setCancelable(mCancelable);
            if (mCancelable) {
                mDialog.setCanceledOnTouchOutside(true);
            }

            if (mOnShowListeners != null) {
                mDialog.setOnShowListeners(mOnShowListeners);
            }

            if (mOnCancelListeners != null) {
                mDialog.setOnCancelListeners(mOnCancelListeners);
            }

            if (mOnDismissListeners != null) {
                mDialog.setOnDismissListeners(mOnDismissListeners);
            }

            if (mOnKeyListener != null) {
                mDialog.setOnKeyListener(mOnKeyListener);
            }

            // 判断有没有设置动画
            if (mAnimations == -1) {
                // 没有的话就设置默认的动画
                mAnimations = AnimStyle.DEFAULT;
            }

            // 设置参数
            WindowManager.LayoutParams params = mDialog.getWindow().getAttributes();
            params.width = mWidth;
            params.height = mHeight;
            params.gravity = mGravity;
            params.windowAnimations = mAnimations;
            params.horizontalMargin = mHorizontalMargin;
            params.verticalMargin = mVerticalMargin;
            mDialog.getWindow().setAttributes(params);

            // 设置文本
            for (int i = 0; i < mTextArray.size(); i++) {
                ((TextView) mContentView.findViewById(mTextArray.keyAt(i))).setText(mTextArray.valueAt(i));
            }

            // 设置可见状态
            for (int i = 0; i < mVisibilityArray.size(); i++) {
                mContentView.findViewById(mVisibilityArray.keyAt(i)).setVisibility(mVisibilityArray.valueAt(i));
            }

            // 设置背景
            for (int i = 0; i < mBackgroundArray.size(); i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mContentView.findViewById(mBackgroundArray.keyAt(i)).setBackground(mBackgroundArray.valueAt(i));
                }else {
                    mContentView.findViewById(mBackgroundArray.keyAt(i)).setBackgroundDrawable(mBackgroundArray.valueAt(i));
                }
            }

            // 设置图片
            for (int i = 0; i < mImageArray.size(); i++) {
                ((ImageView) mContentView.findViewById(mImageArray.keyAt(i))).setImageDrawable(mImageArray.valueAt(i));
            }

            // 设置点击事件
            for (int i = 0; i < mClickArray.size(); i++) {
                mContentView.findViewById(mClickArray.keyAt(i)).setOnClickListener(new ViewClickWrapper(mDialog, mClickArray.valueAt(i)));
            }

            return mDialog;
        }

        /**
         * 创建对话框对象（子类可以重写此方法来改变 Dialog 类型）
         */
        protected BaseDialog createDialog(Context context, int themeResId) {
            return new BaseDialog(context, themeResId);
        }

        /**
         * 显示
         */
        public BaseDialog show() {
            final BaseDialog dialog = create();
            dialog.show();
            return dialog;
        }
    }

    /*
    *    illustration:
    *    DialogInterface接口方法的对象不是Dialog类型，而是DialogInterface类型
    *    此次不想要方法参数是DialogInterface类型，而是用BaseDialog，所以重新定义接口修改参数类型
    *    但是会在Dialog监听的方法报类型转换异常，该异常使用外观设计模式包装类解决
     */
    public interface OnClickListener<V extends View> {
        void onClick(BaseDialog dialog, V view);
    }

    public interface OnShowListener {
        void onShow(BaseDialog dialog);
    }

    public interface OnCancelListener {
        void onCancel(BaseDialog dialog);
    }

    public interface OnDismissListener {
        void onDismiss(BaseDialog dialog);
    }

    /**
     * 点击事件包装类
     */
    private static final class ViewClickWrapper implements View.OnClickListener {

        private final BaseDialog mDialog;
        private final BaseDialog.OnClickListener mListener;

        private ViewClickWrapper(BaseDialog dialog, BaseDialog.OnClickListener listener) {
            mDialog = dialog;
            mListener = listener;
        }

        @SuppressWarnings("unchecked")
        @Override
        public final void onClick(View v) {
            mListener.onClick(mDialog, v);
        }
    }

    /**
     * 显示监听包装类
     */
    private static final class ShowListenerWrapper implements BaseDialog.OnShowListener {

        private final DialogInterface.OnShowListener mListener;

        private ShowListenerWrapper(DialogInterface.OnShowListener listener) {
            mListener = listener;
        }

        @Override
        public void onShow(BaseDialog dialog) {
            mListener.onShow(dialog);
        }
    }

    /**
     * 取消监听包装类
     */
    private static final class CancelListenerWrapper implements BaseDialog.OnCancelListener {

        private final DialogInterface.OnCancelListener mListener;

        private CancelListenerWrapper(DialogInterface.OnCancelListener listener) {
            mListener = listener;
        }

        @Override
        public void onCancel(BaseDialog dialog) {
            mListener.onCancel(dialog);
        }
    }

    /**
     * 销毁监听包装类
     */
    private static final class DismissListenerWrapper implements BaseDialog.OnDismissListener {

        private final DialogInterface.OnDismissListener mListener;

        private DismissListenerWrapper(DialogInterface.OnDismissListener listener) {
            mListener = listener;
        }

        @Override
        public void onDismiss(BaseDialog dialog) {
            mListener.onDismiss(dialog);
        }
    }
}