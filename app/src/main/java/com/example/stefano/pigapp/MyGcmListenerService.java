package com.example.stefano.pigapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import me.leolin.shortcutbadger.ShortcutBadger;

public class MyGcmListenerService extends GcmListenerService {
    private final String TAG = "MyGcmListenerService";
    final static String GROUP_KEY_NOTIFICATIONS = "group_key_notifications";
    final static int MAX_STORED_NOTIFICATIONS=10;
    Context mContext;
    public JSONArray notifications=new JSONArray();


    public void onCreate() {
        mContext = getApplicationContext();
        /*
        //sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences mPrefs = mContext.getSharedPreferences("myAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.clear();
        editor.putInt("notifications", 0);
        editor.commit();*/

    };



    public boolean interestingTopic(Bundle data){
        try {
            JSONObject notification=new JSONObject(""+data.get("notification"));
            String category=notification.getString("category");
            int notificationTopic=-1;
            switch(category){
                case("biblio"):{
                    notificationTopic=1;
                    break;
                }
                case("tess"):{
                    notificationTopic=2;
                    break;
                }
                case("info"):{
                    notificationTopic=3;
                    break;
                }
            }

        int defaultValue = -1;
        int topicsSubscriptions = PreferenceManager.
                getDefaultSharedPreferences(this).getInt("topics", defaultValue);
            Log.d(TAG, "Topic Subscription: "+topicsSubscriptions);
            if(topicsSubscriptions==0){
                return false;
            }
            if(topicsSubscriptions==notificationTopic || notificationTopic==3 || topicsSubscriptions==3){
                return true;
            }
            return false;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {

        if(!interestingTopic(data)){
            Log.d(TAG, "NOT INT");
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy"); //2014/08/06
        Calendar cal = Calendar.getInstance();
        String timestamp=dateFormat.format(cal.getTime());

        SharedPreferences mPrefs = mContext.getSharedPreferences("myAppPrefs", Context.MODE_PRIVATE);
        int defaultValue = -1;
        int numberOfActualNotifications = mPrefs.getInt("notifications", defaultValue);
        Log.d(TAG, "NOTIF: " + numberOfActualNotifications);
        if(numberOfActualNotifications==defaultValue){
            numberOfActualNotifications=0;
            Log.d(TAG, "Initial -1");
        }

        Log.d(TAG, ""+data.get("notification"));
        JSONArray pastNotifications;

        try {
            String notifications = PreferenceManager.
                    getDefaultSharedPreferences(this).getString("pastNotifications", "");
            JSONObject jo;

            if(notifications!= null && !notifications.equals(""))
            {
                Log.d(TAG, "NON vuota -> "+notifications);
                pastNotifications=new JSONArray(notifications);

            }
            else{
                Log.d(TAG,"vuota iu ga");
                pastNotifications=new JSONArray();
            }

            Random r = new Random();
            int nId=r.nextInt(1000);

            JSONObject notification=new JSONObject(""+data.get("notification"));
            String id=notification.getString("id");
            notification.put("date",timestamp);
            notification.put("viewed",false);
            notification.put("notificationID",nId);
            pastNotifications.put(notification);

            int badgeCount = 0;
            int i;
            for(i=0;i<pastNotifications.length();i++){
                JSONObject notif=pastNotifications.getJSONObject(i);
                if(!notif.getBoolean("viewed")){
                    badgeCount++;
                }
            }
            ShortcutBadger.applyCount(mContext, badgeCount); //for 1.1.4
            //ShortcutBadger.with(getApplicationContext()).count(badgeCount); //for 1.1.3

            int numStoredNotifications=pastNotifications.length();
            int outOfBound=numStoredNotifications-MAX_STORED_NOTIFICATIONS;
            if(outOfBound>0){
                for(i=0;i<outOfBound;i++){
                    pastNotifications.remove(0);
                }
            }

            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString("pastNotifications",pastNotifications.toString()).apply();



// Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(MyGcmListenerService.this, NotificationActivity.class);
            resultIntent.putExtra("message", id);
            resultIntent.putExtra("src", 1);

            android.support.v4.app.NotificationCompat.Builder mBuilder =
                    new android.support.v7.app.NotificationCompat.Builder(MyGcmListenerService.this)
                            .setSmallIcon(R.drawable.notifications)
                            .setContentTitle(getString(R.string.notifications_title))
                            .setAutoCancel(true)
                            .setGroup(GROUP_KEY_NOTIFICATIONS)
                            .setContentText(notification.getString("title"))
                            .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                            .setLights(Color.GREEN, 3000, 3000);


// The stack builder object will contain an artificial backs stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(MyGcmListenerService.this);
// Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(NotificationActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);


            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            nId,
                            PendingIntent.FLAG_UPDATE_CURRENT

                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Log.d(TAG, "NotifID: " + nId);
            mNotificationManager.notify(nId, mBuilder.build());
            try {
                Uri not = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone ring = RingtoneManager.getRingtone(getApplicationContext(), not);
                ring.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent in = new Intent("NEW_NOTIFICATION");
        sendBroadcast(in);

    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("GCM Message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());


    }


}
