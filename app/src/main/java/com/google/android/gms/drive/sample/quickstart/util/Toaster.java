package com.google.android.gms.drive.sample.quickstart.util;

import android.content.Context;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by Mikhail Valuyskiy on 07.10.2015.
 */
public class Toaster {
    private static final int SHORT_TOAST_DURATION = 2000;

    private Toaster() {}

    public static void makeLongToast(Context context,String text, long durationInMillis) {
        final Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL, 0, 0);

        new CountDownTimer(Math.max(durationInMillis - SHORT_TOAST_DURATION, 1000), 1000) {
            @Override
            public void onFinish() {
                t.show();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                t.show();
            }
        }.start();
    }
}
