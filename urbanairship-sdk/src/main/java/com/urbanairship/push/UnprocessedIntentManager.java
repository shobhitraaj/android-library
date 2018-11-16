package com.urbanairship.push;

import android.content.Intent;

import com.urbanairship.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shobhit.raj on 02/11/18.
 */

public class UnprocessedIntentManager {

    private static UnprocessedIntentManager mAppUnprocessedIntentManager = null;
    private List<Intent> pendingIntents = new ArrayList<>();

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

    public void setUnProcessedIntent(Intent intent) {
        Logger.verbose("UnprocessedIntentManager - adding intent for post processing: " + intent.getAction());
        pendingIntents.add(intent);
    }

    public List<Intent> getUnProcessedIntents() {
        return pendingIntents;
    }

    public void clearUnprocessedIntents() {
        pendingIntents.clear();
    }

}
