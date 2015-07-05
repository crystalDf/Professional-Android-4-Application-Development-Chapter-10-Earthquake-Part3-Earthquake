package com.star.earthquake;


import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class EarthquakeUpdateService extends IntentService {

    public static String TAG = "EARTHQUAKE_UPDATE_SERVICE";

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    public static final int REQUEST_CODE = 0;

    private Notification.Builder earthquakeNotificationBuilder;
    public static final int NOTIFICATION_ID = 1;

    public EarthquakeUpdateService() {
        super("EarthquakeUpdateService");
    }

    public EarthquakeUpdateService(String name) {
        super(name);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext()
        );

        int updateFreq = Integer.parseInt(sharedPreferences.getString(
                UserPreferenceActivity.PREF_UPDATE_FREQ, "60"));

        boolean autoUpdateChecked = sharedPreferences.getBoolean(
                UserPreferenceActivity.PREF_AUTO_UPDATE, false);

        if (autoUpdateChecked) {

            int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
            long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq * 60 * 1000;

            alarmManager.setInexactRepeating(alarmType, timeToRefresh,
                    updateFreq * 60 * 1000, alarmIntent);
        } else {

            alarmManager.cancel(alarmIntent);

        }

        refreshEarthquakes();

        Bundle args = intent.getExtras();

        if (args != null) {
            Date date = new Date(args.getLong("QuakeDate"));
            String details = args.getString("Details");
            Location location = args.getParcelable("Location");
            Double magnitude = args.getDouble("Magnitude");
            String link = args.getString("Link");

            Quake quake = new Quake(date, details, location, magnitude, link);
            addNewQuake(quake);

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intentToFire = new Intent(EarthquakeAlarmReceiver.ACTION_REFRESH_EARTHQUAKE_ALARM);

        alarmIntent = PendingIntent.getBroadcast(this, REQUEST_CODE,
                intentToFire, 0);

        earthquakeNotificationBuilder = new Notification.Builder(this);
        earthquakeNotificationBuilder
                .setAutoCancel(true)
                .setTicker("Earthquake detected")
                .setSmallIcon(R.drawable.notification_template_icon_bg);
    }

    private void refreshEarthquakes() {

        URL url;

        try {
            url = new URL(getString(R.string.quake_feed));

            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = httpURLConnection.getInputStream();

                DocumentBuilderFactory documentBuilderFactory =
                        DocumentBuilderFactory.newInstance();

                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

                Document document = documentBuilder.parse(inputStream);
                Element element = document.getDocumentElement();

                NodeList nodeList = element.getElementsByTagName("entry");
                if ((nodeList != null) && (nodeList.getLength() > 0)) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Element entry = (Element) nodeList.item(i);

                        Element title = (Element)
                                entry.getElementsByTagName("title").item(0);
                        Element point = (Element)
                                entry.getElementsByTagName("georss:point").item(0);
                        Element when = (Element)
                                entry.getElementsByTagName("updated").item(0);
                        Element link = (Element)
                                entry.getElementsByTagName("link").item(0);

                        if (entry != null && point != null && when != null && link != null) {
                            String details = title.getFirstChild().getNodeValue();

                            String linkString = link.getAttribute("href");

                            String pointString = point.getFirstChild().getNodeValue();

                            String whenString = when.getFirstChild().getNodeValue();

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                    "yyyy-MM-dd'T'hh:mm:ss'Z'"
                            );

                            Date quakeDate = new Date();

                            try {
                                quakeDate = simpleDateFormat.parse(whenString);
                            } catch (ParseException e) {
                                Log.d(TAG, "ParseException");
                            }

                            String[] points = pointString.split(" ");
                            Location location = new Location("dummyGPS");
                            location.setLatitude(Double.parseDouble(points[0]));
                            location.setLongitude(Double.parseDouble(points[0]));

                            String magnitudeString = details.split(" ")[1];
                            int end = magnitudeString.length() - 1;
                            double magnitude = Double.parseDouble(magnitudeString.substring(0, end));

                            details = details.split(",")[1].trim();

                            final Quake quake = new Quake(
                                    quakeDate, details, location, magnitude, linkString
                            );

                            addNewQuake(quake);
                        }
                    }
                }
            }

        } catch (MalformedURLException e) {
            Log.d(TAG, "MalformedURLException");
        } catch (IOException e) {
            Log.d(TAG, "IOException");
        } catch (ParserConfigurationException e) {
            Log.d(TAG, "ParserConfigurationException");
        } catch (SAXException e) {
            Log.d(TAG, "SAXException");
        } finally {

        }

    }

    private void addNewQuake(Quake quake) {

        ContentResolver contentResolver = getContentResolver();

        String selection = EarthquakeProvider.KEY_DATE + " = " + quake.getDate().getTime();

        Cursor cursor = contentResolver.query(EarthquakeProvider.CONTENT_URI, null, selection,
                null, null);

        if (cursor.getCount() == 0) {
            ContentValues contentValues = new ContentValues();

            contentValues.put(EarthquakeProvider.KEY_DATE, quake.getDate().getTime());
            contentValues.put(EarthquakeProvider.KEY_DETAILS, quake.getDetails());
            contentValues.put(EarthquakeProvider.KEY_SUMMARY, quake.toString());

            contentValues.put(EarthquakeProvider.KEY_LOCATION_LAT,
                    quake.getLocation().getLatitude());
            contentValues.put(EarthquakeProvider.KEY_LOCATION_LON,
                    quake.getLocation().getLongitude());

            contentValues.put(EarthquakeProvider.KEY_LINK, quake.getLink());
            contentValues.put(EarthquakeProvider.KEY_MAGNITUDE, quake.getMagnitude());

            broadcastNotification(quake);

            contentResolver.insert(EarthquakeProvider.CONTENT_URI, contentValues);
        }

        cursor.close();
    }

    private void broadcastNotification(Quake quake) {
        Intent startActivityIntent = new Intent(this, EarthquakeActivity.class);
        PendingIntent launchIntent = PendingIntent.getActivity(
                this, REQUEST_CODE, startActivityIntent, 0);

        earthquakeNotificationBuilder
                .setContentIntent(launchIntent)
                .setWhen(quake.getDate().getTime())
                .setContentTitle("M: " + quake.getMagnitude())
                .setContentText(quake.getDetails());

        if (quake.getMagnitude() > 6) {
//            Uri ringURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            earthquakeNotificationBuilder.setSound(ringURI);
            earthquakeNotificationBuilder.setSound(Uri.parse(
                    "file:///storage/emulated/0/BaiduNetdisk/Taylor Swift-Safe And Sound.mp3"));
        }

        double vibrateLength = 100 * Math.exp(0.53 * quake.getMagnitude());
        long[] vibrate = new long[] {1000, (long) vibrateLength, 1000, (long) vibrateLength};
        earthquakeNotificationBuilder.setVibrate(vibrate);

        int color;
        if (quake.getMagnitude() < 5.4) {
            color = Color.GREEN;
        } else if (quake.getMagnitude() < 6) {
            color = Color.YELLOW;
        } else {
            color = Color.RED;
        }

        earthquakeNotificationBuilder.setLights(color, (int) vibrateLength, (int) vibrateLength);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        notificationManager.notify(NOTIFICATION_ID,
                earthquakeNotificationBuilder.build());
    }
}
