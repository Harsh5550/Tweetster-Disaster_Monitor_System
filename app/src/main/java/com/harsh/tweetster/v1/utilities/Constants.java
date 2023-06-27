package com.harsh.tweetster.v1.utilities;

import java.util.HashMap;

public class Constants {

    public static final String CHANNEL_ID="Alert_Channel";
    public static final int NOTIFICATION_ID=100;
    public static final int REQUEST_CODE=100;
    public static final String KEY_COLLECTION_USERS="users";
    public static final String KEY_PERMISSION_FLAG="flag";
    public static final String KEY_COLLECTION_CLUSTER="clusters";
    public static final String KEY_COLLECTION_DISASTERS="disasters";
    public static final String KEY_LATITUDE="latitude";
    public static final String KEY_LONGITUDE="longitude";
    public static final String KEY_LOCATION="location";
    public static final String KEY_ADDRESS="address";
    public static final String KEY_TWEET="tweet";
    public static final String KEY_CLUSTER_ID="clusterId";
    public static final String KEY_DISASTER_NUMBER="disasterNumber";
    public static final String KEY_COUNT="count";
    public static final String KEY_NAME="name";
    public static final String KEY_PHONE_NUMBER="number";
    public static final String KEY_FCM_TOKEN="fcmToken";
    public static final String KEY_PASSWORD="password";
    public static final String KEY_PIN="pin";
    public static final String KEY_TIMESTAMP="timeStamp";
    public static final String KEY_USER_ID="userId";
    public static final String KEY_IS_SIGNED_IN="isSignedIn";
    public static final String KEY_PREFERENCE_NAME="Preference";
    public static final String REMOTE_MSG_AUTHORIZATION="Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE="Content-Type";
    public static final String REMOTE_MSG_DATA="data";
    public static final String REMOTE_MSG_REGISTRATION_IDS="registration_ids";
    public static HashMap<String, String> remoteMsgHeaders=null;
    public static HashMap<String, String> getRemoteMsgHeaders(){
        if (remoteMsgHeaders==null){
            remoteMsgHeaders=new HashMap<>();
            remoteMsgHeaders.put(
                    REMOTE_MSG_AUTHORIZATION,
                    "key=AAAANEGanyQ:APA91bF7R3HMQGtIbit8sg-xX-1uQpGvJGNN4xIqGLmunGz9G2WNcJQtfC7E2tGts33346S0IwMlafl_HCtsaB5HefIcqY1Auj5bp0LWqniLTcRcgaXHijbKNOjNGSUMKqOlidc-z3ZX"
            );
            remoteMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }
}
