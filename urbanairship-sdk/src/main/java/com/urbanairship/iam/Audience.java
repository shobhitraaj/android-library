/* Copyright 2017 Urban Airship and Contributors */

package com.urbanairship.iam;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.urbanairship.UAirship;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonMap;
import com.urbanairship.json.JsonMatcher;
import com.urbanairship.json.JsonPredicate;
import com.urbanairship.json.JsonSerializable;
import com.urbanairship.json.JsonValue;
import com.urbanairship.json.ValueMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Audience conditions for an in-app message. Audiences are normally only validated at display time,
 * and if the audience is not met, the in-app message will be canceled.
 */
public class Audience implements JsonSerializable {

    // JSON keys
    private static final String NEW_USER_KEY = "new_user";
    private static final String NOTIFICATION_OPT_IN_KEY = "notification_opt_in";
    private static final String LOCATION_OPT_IN_KEY = "location_opt_in";
    private static final String LOCALE_KEY = "locale";
    private static final String APP_VERSION_KEY = "app_version";
    private static final String TAGS_KEY = "tags";
    private static final String TEST_DEVICES_KEY = "test_devices";


    // Platform keys for app version
    static final String AMAZON_VERSION_KEY = "amazon";
    static final String ANDROID_VERSION_KEY = "android";

    private final Boolean newUser;
    private final Boolean notificationsOptIn;
    private final Boolean locationOptIn;
    private final List<String> languageTags;
    private final List<String> testDevices;
    private final TagSelector tagSelector;
    private final JsonPredicate versionPredicate;

    /***
     * Default constructor.
     * @param builder The builder.
     */
    private Audience(Builder builder) {
        this.newUser = builder.newUser;
        this.notificationsOptIn = builder.notificationsOptIn;
        this.locationOptIn = builder.locationOptIn;
        this.languageTags = builder.languageTags;
        this.tagSelector = builder.tagSelector;
        this.versionPredicate = builder.versionPredicate;
        this.testDevices = builder.testDevices;
    }

    @Override
    public JsonValue toJsonValue() {
        return JsonMap.newBuilder()
                      .putOpt(NEW_USER_KEY, newUser)
                      .putOpt(NOTIFICATION_OPT_IN_KEY, notificationsOptIn)
                      .putOpt(LOCATION_OPT_IN_KEY, locationOptIn)
                      .put(LOCALE_KEY, languageTags.isEmpty() ? null : JsonValue.wrapOpt(languageTags))
                      .put(TEST_DEVICES_KEY, testDevices.isEmpty() ? null : JsonValue.wrapOpt(testDevices))
                      .put(TAGS_KEY, tagSelector)
                      .put(APP_VERSION_KEY, versionPredicate)
                      .build().toJsonValue();
    }

    /**
     * Parses the json value.
     *
     * @param jsonValue The json value.
     * @return The audience condition.
     * @throws JsonException If the json is invalid.
     */
    public static Audience parseJson(JsonValue jsonValue) throws JsonException {
        JsonMap content = jsonValue.optMap();

        Builder builder = newBuilder();

        // New User
        if (content.containsKey(NEW_USER_KEY)) {
            if (!content.get(NEW_USER_KEY).isBoolean()) {
                throw new JsonException("new_user must be a boolean: " + content.get(NEW_USER_KEY));
            }
            builder.setNewUser(content.get(NEW_USER_KEY).getBoolean(false));
        }

        // Push Opt-in
        if (content.containsKey(NOTIFICATION_OPT_IN_KEY)) {
            if (!content.get(NOTIFICATION_OPT_IN_KEY).isBoolean()) {
                throw new JsonException("notification_opt_in must be a boolean: " + content.get(NOTIFICATION_OPT_IN_KEY));
            }
            builder.setNotificationsOptIn(content.get(NOTIFICATION_OPT_IN_KEY).getBoolean(false));
        }

        // Location Opt-in
        if (content.containsKey(LOCATION_OPT_IN_KEY)) {
            if (!content.get(LOCATION_OPT_IN_KEY).isBoolean()) {
                throw new JsonException("location_opt_in must be a boolean: " + content.get(LOCATION_OPT_IN_KEY));
            }
            builder.setLocationOptIn(content.get(LOCATION_OPT_IN_KEY).getBoolean(false));
        }

        // Locale
        if (content.containsKey(LOCALE_KEY)) {
            if (!content.get(LOCALE_KEY).isJsonList()) {
                throw new JsonException("locales must be an array: " + content.get(LOCALE_KEY));
            }

            for (JsonValue value : content.opt(LOCALE_KEY).optList()) {
                if (!value.isString()) {
                    throw new JsonException("Invalid locale: " + value);
                }

                builder.addLanguageTag(value.getString());
            }
        }

        // App Version
        if (content.containsKey(APP_VERSION_KEY)) {
            builder.setVersionPredicate(JsonPredicate.parse(content.get(APP_VERSION_KEY)));
        }

        // Tags
        if (content.containsKey(TAGS_KEY)) {
            builder.setTagSelector(TagSelector.parseJson(content.get(TAGS_KEY)));
        }

        // Test devices
        if (content.containsKey(TEST_DEVICES_KEY)) {
            if (!content.get(TEST_DEVICES_KEY).isJsonList()) {
                throw new JsonException("test devices must be an array: " + content.get(LOCALE_KEY));
            }

            for (JsonValue value : content.opt(TEST_DEVICES_KEY).optList()) {
                if (!value.isString()) {
                    throw new JsonException("Invalid test device: " + value);
                }

                builder.addTestDevice(value.getString());
            }
        }

        return builder.build();
    }

    /**
     * Gets the list of language tags.
     *
     * @return A list of language tags.
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    List<String> getLanguageTags() {
        return languageTags;
    }

    /**
     * Gets the list of test devices.
     *
     * @return A list of test devices.
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    List<String> getTestDevices() {
        return testDevices;
    }

    /**
     * Gets the notification opt-in status.
     *
     * @return The notification opt-in status.
     */
    @Nullable
    Boolean getNotificationsOptIn() {
        return notificationsOptIn;
    }

    /**
     * Gets the location opt-in status.
     *
     * @return The location opt-in status.
     */
    @Nullable
    Boolean getLocationOptIn() {
        return locationOptIn;
    }

    /**
     * Gets the new user status.
     *
     * @return The new user status.
     */
    @Nullable
    Boolean getNewUser() {
        return newUser;
    }

    /**
     * Gets the tag selector.
     *
     * @return The tag selector.
     */
    @Nullable
    TagSelector getTagSelector() {
        return tagSelector;
    }

    /**
     * Gets the app version predicate.
     *
     * @return The app version predicate.
     */
    @Nullable
    JsonPredicate getVersionPredicate() {
        return versionPredicate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Audience audience = (Audience) o;

        if (newUser != null ? !newUser.equals(audience.newUser) : audience.newUser != null) {
            return false;
        }
        if (notificationsOptIn != null ? !notificationsOptIn.equals(audience.notificationsOptIn) : audience.notificationsOptIn != null) {
            return false;
        }
        if (locationOptIn != null ? !locationOptIn.equals(audience.locationOptIn) : audience.locationOptIn != null) {
            return false;
        }
        if (languageTags != null ? !languageTags.equals(audience.languageTags) : audience.languageTags != null) {
            return false;
        }
        if (tagSelector != null ? !tagSelector.equals(audience.tagSelector) : audience.tagSelector != null) {
            return false;
        }
        return versionPredicate != null ? versionPredicate.equals(audience.versionPredicate) : audience.versionPredicate == null;
    }

    @Override
    public int hashCode() {
        int result = newUser != null ? newUser.hashCode() : 0;
        result = 31 * result + (notificationsOptIn != null ? notificationsOptIn.hashCode() : 0);
        result = 31 * result + (locationOptIn != null ? locationOptIn.hashCode() : 0);
        result = 31 * result + (languageTags != null ? languageTags.hashCode() : 0);
        result = 31 * result + (tagSelector != null ? tagSelector.hashCode() : 0);
        result = 31 * result + (versionPredicate != null ? versionPredicate.hashCode() : 0);
        return result;
    }

    /**
     * Builder factory method.
     *
     * @return A new builder instance.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Audience builder.
     */
    public static class Builder {

        private Boolean newUser;
        private Boolean notificationsOptIn;
        private Boolean locationOptIn;
        private final List<String> languageTags = new ArrayList<>();
        private final List<String> testDevices = new ArrayList<>();

        private TagSelector tagSelector;
        private JsonPredicate versionPredicate;

        private Builder() {}

        /**
         * Sets the new user audience condition for scheduling the in-app message.
         *
         * @param newUser {@code true} if only new users should schedule the in-app message, otherwise {@code false}.
         * @return The builder.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        Builder setNewUser(boolean newUser) {
            this.newUser = newUser;
            return this;
        }

        /**
         * Adds a test device.
         * @param hash The hashed channel.
         * @return THe builder.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        Builder addTestDevice(String hash) {
            this.testDevices.add(hash);
            return this;
        }

        /**
         * Sets the location opt-in audience condition for the in-app message.
         *
         * @param optIn {@code true} if location must be opted in, otherwise {@code false}.
         * @return The builder.
         * @hide
         */
        public Builder setLocationOptIn(boolean optIn) {
            this.locationOptIn = optIn;
            return this;
        }

        /**
         * Sets the notification opt-in audience condition for the in-app message.
         *
         * @param optIn {@code true} if notifications must be opted in, otherwise {@code false}.
         * @return The builder.
         * @hide
         */
        public Builder setNotificationsOptIn(boolean optIn) {
            this.notificationsOptIn = optIn;
            return this;
        }

        /**
         * Adds a BCP 47 location tag. Only the language and country code are used
         * to determine the audience.
         *
         * @param languageTag A BCP 47 language tag.
         * @return The builder.
         */
        public Builder addLanguageTag(@NonNull String languageTag) {
            languageTags.add(languageTag);
            return this;
        }

        /**
         * Value predicate to be used to match the app's version int.
         *
         * @param predicate Json predicate to match the version object.
         * @return The builder.
         */
        private Builder setVersionPredicate(JsonPredicate predicate) {
            this.versionPredicate = predicate;
            return this;
        }

        /**
         * Value matcher to be used to match the app's version int.
         *
         * @param valueMatcher Value matcher to be applied to the app's version int.
         * @return The builder.
         */
        public Builder setVersionMatcher(ValueMatcher valueMatcher) {

            String platform;
            switch (UAirship.shared().getPlatformType()) {
                case UAirship.AMAZON_PLATFORM:
                    platform = AMAZON_VERSION_KEY;
                    break;
                case UAirship.ANDROID_PLATFORM:
                default:
                    platform = ANDROID_VERSION_KEY;
                    break;
            }

            JsonPredicate predicate = JsonPredicate.newBuilder()
                                                   .addMatcher(JsonMatcher.newBuilder()
                                                                          .setKey(platform)
                                                                          .setValueMatcher(valueMatcher)
                                                                          .build())
                                                   .build();

            return setVersionPredicate(predicate);
        }

        /**
         * Sets the tag selector. Tag selector will only be applied to channel tags set through
         * the SDK.
         *
         * @param tagSelector The tag selector.
         * @return The builder.
         */
        public Builder setTagSelector(TagSelector tagSelector) {
            this.tagSelector = tagSelector;
            return this;
        }

        /**
         * Builds the in-app message audience.
         *
         * @return The audience.
         */
        public Audience build() {
            return new Audience(this);
        }
    }
}
