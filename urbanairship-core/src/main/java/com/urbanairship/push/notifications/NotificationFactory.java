/* Copyright 2018 Urban Airship and Contributors */

package com.urbanairship.push.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

import com.urbanairship.AirshipReceiver;
import com.urbanairship.CoreReceiver;
import com.urbanairship.Logger;
import com.urbanairship.R;
import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;
import com.urbanairship.push.PushMessage;
import com.urbanairship.util.NotificationIdGenerator;
import com.urbanairship.util.UAStringUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Notification factory that provides a pathway for customizing the display of push notifications
 * in the Android <code>NotificationManager</code>.
 * <p/>
 * {@link DefaultNotificationFactory} is used by default and applies the big text style. For custom
 * layouts, see {@link CustomLayoutNotificationFactory}.
 */
public class NotificationFactory {

    public static final String DEFAULT_NOTIFICATION_CHANNEL = "com.urbanairship.default";

    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private int titleId;
    private int smallIconId;
    private int largeIcon;
    private Uri sound = null;
    private int constantNotificationId = -1;
    private int accentColor = NotificationCompat.COLOR_DEFAULT;
    private int notificationDefaults = NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE;

    /**
     * Default Notification ID when the {@link PushMessage} defines a notification tag.
     */
    public static final int TAG_NOTIFICATION_ID = 100;


    /**
     * A container for a NotificationFactory result, containing a nullable
     * Notification instance and a status code.
     */
    public static class Result {

        @IntDef({ OK, RETRY, CANCEL })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Status {}

        /**
         * Indicates that a Notification was successfully created.
         */
        public static final int OK = 0;

        /**
         * Indicates that a Notification was not successfully created and that a job should be
         * scheduled to retry later.
         */
        public static final int RETRY = 1;

        /**
         * Indicates that a Notification was not successfully created and no work should be scheduled
         * to retry.
         */
        public static final int CANCEL = 2;

        private Notification notification;
        private @Status
        int status;

        /**
         * NotificationFactory.Result constructor.
         *
         * @param notification The Notification.
         * @param status The status.
         */
        private Result(Notification notification, @Status int status) {
            this.notification = notification;

            if (notification == null && status == OK) {
                this.status = CANCEL;
            } else {
                this.status = status;
            }
        }

        /**
         * Creates a new result containing a notification.
         *
         * @param notification The notification.
         * @return An instance of NotificationFactory.Result.
         */
        public static Result notification(@NonNull Notification notification) {
            return new Result(notification, OK);
        }

        /**
         * Creates a new result with a <code>CANCEL</code> status code.
         *
         * @return An instance of NotificationFactory.Result.
         */
        public static Result cancel() {
            return new Result(null, CANCEL);
        }

        /**
         * Creates a new result with a <code>RETRY</code> status code.
         *
         * @return An instance of NotificationFactory.Result.
         */
        public static Result retry() {
            return new Result(null, RETRY);
        }

        /**
         * Gets the Notification.
         *
         * @return The Notification.
         */
        public Notification getNotification() { return notification; }

        /**
         * Gets the status.
         *
         * @return The status.
         */
        public @Status
        int getStatus() { return status; }
    }

    private final Context context;
    private String notificationChannel;

    /**
     * Default constructor.
     *
     * @param context The application context.
     */
    public NotificationFactory(@NonNull Context context) {
        this.context = context.getApplicationContext();
        titleId = context.getApplicationInfo().labelRes;
        smallIconId = context.getApplicationInfo().icon;
    }

    /**
     * Set the optional constant notification ID.
     * <p>
     * Only values greater than 0 will be used by default. Any negative value will
     * be considered invalid and the constant notification ID will be ignored.
     * <p>
     * By default, the constant notification ID will be used if the push message does not contain a tag.
     * In that case, {@link #TAG_NOTIFICATION_ID} will be used instead.
     *
     * @param id The integer ID as an int.
     */
    public NotificationFactory setConstantNotificationId(int id) {
        constantNotificationId = id;
        return this;
    }

    /**
     * Get the constant notification ID.
     *
     * @return The constant notification ID as an int.
     */
    public int getConstantNotificationId() {
        return constantNotificationId;
    }

    /**
     * Set the title used in the notification layout.
     *
     * @param titleId The title as an int. A value of -1 will not display a title. A value of 0 will
     * display the application name as the title. A string resource ID will display the specified
     * string as the title.
     */
    public void setTitleId(@StringRes int titleId) {
        this.titleId = titleId;
    }

    /**
     * Get the title used in the notification layout.
     *
     * @return The title as an int.
     */
    @StringRes
    public int getTitleId() {
        return titleId;
    }

    /**
     * Set the small icon used in the notification layout.
     *
     * @param smallIconId The small icon ID as an int.
     */
    public void setSmallIconId(@DrawableRes int smallIconId) {
        this.smallIconId = smallIconId;
    }

    /**
     * Get the small icon used in the notification layout.
     *
     * @return The small icon ID as an int.
     */
    @DrawableRes
    public int getSmallIconId() {
        return smallIconId;
    }

    /**
     * Set the sound played when the notification arrives.
     *
     * @param sound The sound as a Uri.
     */
    public void setSound(Uri sound) {
        this.sound = sound;
    }

    /**
     * Get the sound played when the notification arrives.
     *
     * @return The sound as a Uri.
     */
    public Uri getSound() {
        return sound;
    }

    /**
     * Set the large icon used in the notification layout.
     *
     * @param largeIcon The large icon ID as an int.
     */
    public void setLargeIcon(@DrawableRes int largeIcon) {
        this.largeIcon = largeIcon;
    }

    /**
     * Get the large icon used in the notification layout.
     *
     * @return The large icon ID as a int.
     */
    @DrawableRes
    public int getLargeIcon() {
        return largeIcon;
    }

    /**
     * Set the accent color used in the notification.
     *
     * @param accentColor The accent color of the main notification icon.
     */
    public void setColor(@ColorInt int accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Get the accent color used in the notification.
     *
     * @return The accent color as an int.
     */
    @ColorInt
    public int getColor() {
        return accentColor;
    }

    /**
     * Gets the default notification options.
     *
     * @return The default notification options.
     */
    public int getNotificationDefaultOptions() {
        return notificationDefaults;
    }

    /**
     * Sets the default notification options. Defaults to
     * {@code NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE}.
     *
     * @param defaults The default options.
     */
    public void setNotificationDefaultOptions(int defaults) {
        this.notificationDefaults = defaults;
    }

    /**
     * Sets the default notification channel.
     *
     * @param channel THe default notification channel.
     */
    public void setNotificationChannel(String channel) {
        this.notificationChannel = channel;
    }

    /**
     * Gets the default notification channel.
     *
     * @return The default notification channel.
     */
    public String getNotificationChannel() {
        return this.notificationChannel;
    }

    /**
     * Gets the default title for the notification. If the {@link #getTitleId()} is 0,
     * the application label will be used, if greater than 0 the string will be fetched
     * from the resources, and if negative an empty String
     *
     * @return The default notification title.
     */
    protected String getTitle(@NonNull PushMessage message) {
        if (message.getTitle() != null) {
            return message.getTitle();
        }

        if (getTitleId() == 0) {
            return getContext().getPackageManager().getApplicationLabel(getContext().getApplicationInfo()).toString();
        } else {
            return getContext().getString(getTitleId());
        }
    }

    /**
     * Gets application context.
     *
     * @return The application context.
     */
    @NonNull
    protected Context getContext() {
        return context;
    }

    /**
     * Creates a <code>Notification</code> for an incoming push message.
     * <p/>
     * In order to handle notification opens, the application should register a broadcast receiver
     * that extends {@link AirshipReceiver}. When the notification is opened
     * it will call {@link AirshipReceiver#onNotificationOpened(Context, AirshipReceiver.NotificationInfo)}
     * giving the application a chance to handle the notification open. If the broadcast receiver is not registered,
     * or {@code false} is returned, an open will be handled by either starting the launcher activity or
     * by sending the notification's content intent if it is present.
     *
     * @param message The push message.
     * @param notificationId The notification ID.
     * @return The notification to display, or <code>null</code> if no notification is desired.
     */
    @Nullable
    public Notification createNotification(@NonNull final PushMessage message, final int notificationId) {
        if (UAStringUtil.isEmpty(message.getAlert())) {
            return null;
        }

        NotificationCompat.Builder builder = createNotificationBuilder(message, notificationId, null);
        return builder.build();
    }

    /**
     * Creates a <code>Notification</code> for an incoming push message.
     * <p/>
     * In order to handle notification opens, the application should register a broadcast receiver
     * that extends {@link AirshipReceiver}. When the notification is opened
     * it will call {@link AirshipReceiver#onNotificationOpened(Context, AirshipReceiver.NotificationInfo)}
     * giving the application a chance to handle the notification open. If the broadcast receiver is not registered,
     * or {@code false} is returned, an open will be handled by either starting the launcher activity or
     * by sending the notification's content intent if it is present.
     *
     * @param message The push message.
     * @param notificationId The notification ID.
     * @return The notification result.
     * @deprecated Use {@link #createNotificationResult(PushMessage, int, boolean)}.
     */
    @NonNull
    @Deprecated
    public Result createNotificationResult(@NonNull final PushMessage message, final int notificationId) {
        Notification notification = createNotification(message, notificationId);
        if (notification == null) {
            return Result.cancel();
        } else {
            return Result.notification(notification);
        }
    }


    /**
     * Creates a <code>Notification</code> for an incoming push message.
     * <p/>
     * In order to handle notification opens, the application should register a broadcast receiver
     * that extends {@link AirshipReceiver}. When the notification is opened
     * it will call {@link AirshipReceiver#onNotificationOpened(Context, AirshipReceiver.NotificationInfo)}
     * giving the application a chance to handle the notification open. If the broadcast receiver is not registered,
     * or {@code false} is returned, an open will be handled by either starting the launcher activity or
     * by sending the notification's content intent if it is present.
     *
     * @param message The push message.
     * @param notificationId The notification ID.
     * @param isLongRunningTask {@code true} if the factory is currently running using a job and has
     * extended background time to create the notification result. {@code false} if the factory has
     * limited background time and should create the notification within 10 seconds. Note, 10 seconds
     * is for total background time. This includes Application start time and any time spent in the
     * onReady callback during takeOff.
     * @return The notification result.
     */
    @NonNull
    public Result createNotificationResult(@NonNull final PushMessage message, final int notificationId, boolean isLongRunningTask) {
        //noinspection deprecation
        return this.createNotificationResult(message, notificationId);
    }

    /**
     * Creates a NotificationCompat.Builder with the default settings applied.
     *
     * @param message The PushMessage.
     * @param notificationId The notification id.
     * @param defaultStyle The default notification style.
     * @return A NotificationCompat.Builder.
     */
    protected NotificationCompat.Builder createNotificationBuilder(@NonNull PushMessage message, int notificationId, @Nullable NotificationCompat.Style defaultStyle) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext())
                .setContentTitle(getTitle(message))
                .setContentText(message.getAlert())
                .setAutoCancel(true)
                .setLocalOnly(message.isLocalOnly())
                .setColor(message.getIconColor(getColor()))
                .setSmallIcon(message.getIcon(context, getSmallIconId()))
                .setPriority(message.getPriority())
                .setCategory(message.getCategory())
                .setVisibility(message.getVisibility());


        if (Build.VERSION.SDK_INT < 26) {
            int defaults = getNotificationDefaultOptions();

            if (message.getSound(getContext()) != null) {
                builder.setSound(message.getSound(getContext()));

                // Remove the Notification.DEFAULT_SOUND flag
                defaults &= ~Notification.DEFAULT_SOUND;
            } else if (getSound() != null) {
                builder.setSound(getSound());

                // Remove the Notification.DEFAULT_SOUND flag
                defaults &= ~Notification.DEFAULT_SOUND;
            }
            builder.setDefaults(defaults);
        }


        if (getLargeIcon() != 0) {
            builder.setLargeIcon(BitmapFactory.decodeResource(getContext().getResources(), getLargeIcon()));
        }

        if (message.getSummary() != null) {
            builder.setSubText(message.getSummary());
        }

        // Public notification
        builder.extend(new PublicNotificationExtender(getContext(), message)
                .setAccentColor(getColor())
                .setLargeIcon(getLargeIcon())
                .setSmallIcon(getSmallIconId()));


        // Wearable support
        builder.extend(new WearableNotificationExtender(getContext(), message, notificationId));

        // Notification action buttons
        builder.extend(new ActionsNotificationExtender(getContext(), message, notificationId));

        // Styles
        builder.extend(new StyleNotificationExtender(getContext(), message)
                .setDefaultStyle(defaultStyle));

        // Channels for Android O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(getActiveNotificationChannel(message));
        }

        return builder;
    }

    /**
     * Creates a notification ID based on the message and payload.
     * <p/>
     * This method could return a constant (to always replace the existing ID)
     * or a payload/message specific ID (to replace in cases where there are duplicates, for example)
     * or a random/sequential (to always add a new notification).
     * <p>
     * The default behavior returns {@link #TAG_NOTIFICATION_ID} if the push message contains a tag
     * (see {@link PushMessage#getNotificationTag()}). Otherwise it will either return {@link #getConstantNotificationId()}
     * if the constant notification id > 0, or it will return a randomly generated ID}.
     *
     * @param pushMessage The push message.
     * @return An integer ID for the next notification.
     */
    public int getNextId(@NonNull PushMessage pushMessage) {
        int id;
        if (pushMessage.getNotificationTag() != null) {
            id =  TAG_NOTIFICATION_ID;
        }else if (constantNotificationId > 0) {
            id =  constantNotificationId;
        }else {
            id = NotificationIdGenerator.nextID();
        }
        pushMessage.putValue(PushMessage.EXTRA_NOTIFICATION_ID,Integer.toString(id));
        return id;
    }


    /**
     * Gets the active notification channel for the push message. If neither {@link PushMessage#getNotificationChannel()} or {@link #getNotificationChannel()} result
     * in an active channel, {@link #DEFAULT_NOTIFICATION_CHANNEL} will be used.
     *
     * @param message The push message.
     * @return The active notification channel from the message, factory, or the default channel.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    String getActiveNotificationChannel(PushMessage message) {
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (message.getNotificationChannel() != null) {
            String channel = message.getNotificationChannel();
            if (notificationManager.getNotificationChannel(channel) != null) {
                return channel;
            }

            Logger.error("Message notification channel " + message.getNotificationChannel() + " does not exist. Unable to apply channel on notification.");
        }

        if (getNotificationChannel() != null) {
            String channel = getNotificationChannel();
            if (notificationManager.getNotificationChannel(channel) != null) {
                return channel;
            }

            Logger.error("Factory notification channel " + getNotificationChannel() + " does not exist. Unable to apply channel on notification.");
        }


        // Fallback to Default Channel
        NotificationChannel channel = new NotificationChannel(DEFAULT_NOTIFICATION_CHANNEL,
                context.getString(R.string.ua_default_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);

        channel.setDescription(context.getString(R.string.ua_default_channel_description));

        notificationManager.createNotificationChannel(channel);

        return DEFAULT_NOTIFICATION_CHANNEL;
    }

    /**
     * Checks if the push message requires a long running task. If {@code true}, the push message
     * will be scheduled to process at a later time when the app has more background time. If {@code false},
     * the app has approximately 10 seconds to post the notification in {@link #createNotification(PushMessage, int)}
     * and {@link #getNextId(PushMessage)}.
     * <p>
     * Apps that return {@code false} are highly encouraged to add {@code RECEIVE_BOOT_COMPLETED} so
     * the push message will persist between device reboots.
     *
     * @param message The push message.
     * @return {@code true} to require long running task, otherwise {@code false}.
     */
    public boolean requiresLongRunningTask(PushMessage message) {
        return false;
    }

    /**
     * Posts the notification
     *
     * @param airship        The airship instance.
     * @param notification   The notification.
     * @param notificationId The notification ID.
     */
    public void postNotification(UAirship airship, Notification notification, int notificationId,PushMessage message) {

        if (Build.VERSION.SDK_INT < 26) {
            if (!airship.getPushManager().isVibrateEnabled() || airship.getPushManager().isInQuietTime()) {
                // Remove both the vibrate and the DEFAULT_VIBRATE flag
                notification.vibrate = null;
                notification.defaults &= ~Notification.DEFAULT_VIBRATE;
            }

            if (!airship.getPushManager().isSoundEnabled() || airship.getPushManager().isInQuietTime()) {
                // Remove both the sound and the DEFAULT_SOUND flag
                notification.sound = null;
                notification.defaults &= ~Notification.DEFAULT_SOUND;
            }
        }

        Intent contentIntent = new Intent(context, CoreReceiver.class)
                .setAction(PushManager.ACTION_NOTIFICATION_OPENED_PROXY)
                .addCategory(UUID.randomUUID().toString())
                .putExtra(PushManager.EXTRA_PUSH_MESSAGE_BUNDLE, message.getPushBundle())
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra(PushManager.EXTRA_NOTIFICATION_ID, notificationId);

        // If the notification already has an intent, add it to the extras to be sent later
        if (notification.contentIntent != null) {
            contentIntent.putExtra(PushManager.EXTRA_NOTIFICATION_CONTENT_INTENT, notification.contentIntent);
        }

        Intent deleteIntent = new Intent(context, CoreReceiver.class)
                .setAction(PushManager.ACTION_NOTIFICATION_DISMISSED_PROXY)
                .addCategory(UUID.randomUUID().toString())
                .putExtra(PushManager.EXTRA_PUSH_MESSAGE_BUNDLE, message.getPushBundle())
                .putExtra(PushManager.EXTRA_NOTIFICATION_ID, notificationId);

        if (notification.deleteIntent != null) {
            deleteIntent.putExtra(PushManager.EXTRA_NOTIFICATION_DELETE_INTENT, notification.deleteIntent);
        }

        notification.contentIntent = PendingIntent.getBroadcast(context, 0, contentIntent, 0);
        notification.deleteIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);

    }

}
