package com.urbanairship.push;

import android.content.Intent;

/**
 * Created by shobhit.raj on 02/11/18.
 */

public class UnprocessedIntentManager {

    private static UnprocessedIntentManager mAppUnprocessedIntentManager = null;
    private Intent pendingIntent = null;

    private UnprocessedIntentManager() {

    }

    public static UnprocessedIntentManager getAppUnprocessedIntentManager() {
        synchronized (UnprocessedIntentManager.class) {
            if (mAppUnprocessedIntentManager == null) {
                mAppUnprocessedIntentManager = new UnprocessedIntentManager();
            }
        }
        return mAppUnprocessedIntentManager;
    }

    public void setPendingIntent(Intent intent) {
        this.pendingIntent = intent;
    }

    public Intent getPendingIntent() {
        return pendingIntent;
    }

    public void clearPendingIntent() {
        pendingIntent = null;
    }

}
