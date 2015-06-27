package th.demo;

import android.os.Handler;

import th.pd.common.android.ProgressArc;
import th.pd.common.android.ProgressArc.ProgressChangeListener;

public class ProgressArcDemo {

    private static final int MSG_INCREASE = 0;

    private ProgressArc mProgArc;

    private Handler mHandler;

    public ProgressArcDemo(ProgressArc progArc) {
        mProgArc = progArc;
        mProgArc.setListener(
                new ProgressChangeListener() {

                    @Override
                    public void onChange(float value) {
                    }

                    @Override
                    public void onComplete(float value) {
                        mProgArc.setValue(0);
                    }
                });

        mHandler = new Handler(progArc.getContext().getMainLooper()) {

            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_INCREASE:
                        autoIncrease();
                        start();
                        break;
                }
            };
        };
    }

    private void autoIncrease() {
        mProgArc.setValue(mProgArc.getValue() + 0.02f);
    }

    public void start() {
        mHandler.sendEmptyMessageDelayed(MSG_INCREASE, 100);
    }
}
