package com.urbanairship;

import com.urbanairship.push.PushMessage;

/**
 * Created by shobhit.raj on 25/11/18.
 */


public interface UaNotificationOpenCallback {

    public void intentReceived(PushMessage message);
}
