package com.urbanairship;

import com.urbanairship.push.UaNotificationOpenCallback;
import com.urbanairship.push.UnprocessedIntentManager;

/**
 * Created by shobhit.raj on 20/11/18.
 */

public class NpNotificationClickCallBack {

    private static NpNotificationClickCallBack npNotificationClickCallBack;

    private UaNotificationOpenCallback uaNotificationOpenCallback;

    private NpNotificationClickCallBack(){
    }

    public static NpNotificationClickCallBack getInstance(){
        synchronized (NpNotificationClickCallBack.class) {
            if(npNotificationClickCallBack == null){
                npNotificationClickCallBack = new NpNotificationClickCallBack();
            }
        }
        return npNotificationClickCallBack;
    }

    public UaNotificationOpenCallback getUaNotificationOpenCallback() {
        return uaNotificationOpenCallback;
    }

    public void setUaNotificationOpenCallback(UaNotificationOpenCallback uaNotificationOpenCallback) {
        this.uaNotificationOpenCallback = uaNotificationOpenCallback;
    }
}
