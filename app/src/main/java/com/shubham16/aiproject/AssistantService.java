package com.shubham16.aiproject;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class AssistantService extends Service {
    private static final String TAG = "AssistantService";
    private static final int RE_CODE = 32;
    private static final String NOTIFICATION_CHANNEL_ID = "My_Not_Channel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Name of channel";
    private static final String NOTIFICATION_CHANNEL_DESC = "Description of channel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private android.speech.SpeechRecognizer androidRecognizer;
    private Intent recognizerIntent;
    private boolean isPressedOnce = false, isListening = false;
    private int falseReceive = 0;
    private AudioManager audioManager;
    private Vibrator vibe;
    private TextToSpeech myTTS;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "This", Toast.LENGTH_SHORT).show();
        showNotification();

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initializeTTS();
        setRecogniserIntent();
        androidRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
        setupAndroidRecognizer();

        setInvokeBroadCasts();

        return START_STICKY;
    }

    private void playSound(boolean isStart){
        MediaPlayer mPlayer = MediaPlayer.create(getApplicationContext(), isStart ? R.raw.mstart : R.raw.mstop);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setVolume(100, 100);
        mPlayer.start();
    }

    private void setInvokeBroadCasts() {
        SensorManager sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor sensor = sensorMan.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorMan.registerListener(
                new SensorEventListener() {

                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                            Log.d("tagtag", "proximity");
                            float value0 = sensorEvent.values.length > 0 ? sensorEvent.values[0] : 1554f;
                            Log.d("tagtag", "length " + sensorEvent.values.length);
                            Log.d("tagtag", "value_ " + value0);
                            Log.d("tagtag", "values " + Arrays.toString(sensorEvent.values));
                            Log.d("tagtag", "accuracy " + sensorEvent.accuracy);
                            Log.d("tagtag", "... ");

                            if (sensorEvent.values.length > 0) {
                                handleVolumePressed();
                            }
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }
                }, sensor,
                SensorManager.SENSOR_DELAY_NORMAL);


    }

    private void handleVolumePressed() {
        Log.d(TAG, "handleVolumePressed: called");
        if (isListening)
            return;
        falseReceive++;
        if (falseReceive != 2)
            return;

        falseReceive = 0;
        if (!isPressedOnce) {
            isPressedOnce = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isPressedOnce = false;
                }
            }, 1000);
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (vibe != null) {
            vibe.vibrate(200);
        }
        playSound(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                audioManager.setStreamMute(AudioManager.STREAM_RING, true);
                androidRecognizer.startListening(recognizerIntent);
            }
        }, 200);
    }

    private void initializeTTS() {
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (myTTS.getEngines().size() == 0) {
                    Toast.makeText(AssistantService.this, "There is no TTS", Toast.LENGTH_SHORT).show();
                } else {
                    myTTS.setLanguage(Locale.forLanguageTag("en-in"));
                    myTTS.setVoice(new Voice("en-in-x-ahp#male_3-local", Locale.forLanguageTag("en-in"), Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, null));
                }
            }
        });
    }

    private void speak(String text) {
        myTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    //setup the android recogniser
    private void setRecogniserIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
    }

    public void setupAndroidRecognizer() {
        if (android.speech.SpeechRecognizer.isRecognitionAvailable(this)) {
            androidRecognizer.setRecognitionListener(androidListener);
            return;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myTTS.shutdown();
        if (androidRecognizer != null) {
            androidRecognizer.cancel();
            androidRecognizer.destroy();
        }
        Toast.makeText(this, "Destroy", Toast.LENGTH_SHORT).show();
    }

    private android.speech.RecognitionListener androidListener = new android.speech.RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float v) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onPartialResults(Bundle bundle) {

        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }

        @Override
        public void onError(int n) {
            processListeningResult(null);
        }

        @Override
        public void onResults(Bundle bundle) {
            final ArrayList<String> arrayList = bundle.getStringArrayList("results_recognition");
            if (arrayList != null && !arrayList.isEmpty())
                processListeningResult(arrayList.get(0));
        }
    };

    private void processListeningResult(final String result) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                audioManager.setStreamMute(AudioManager.STREAM_RING, false);
                isListening = false;
                if (result != null) {
                    action(result.toLowerCase());
                }
            }
        }, 500);
        if (result == null)
            playSound(false);
            errorVibrate();
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("AI Assistant")
                .setTicker("TICKER")
                .setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setColor(Color.WHITE);
            builder.setSmallIcon(R.drawable.assist);
        } else {
            builder.setSmallIcon(R.mipmap.ic_mob);
        }
        builder.setContentText("Hover your hand 2 times over proximity sensor");

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
        startForeground(12, notification);
    }

    private void errorVibrate() {
        doneVibrate();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doneVibrate();
            }
        }, 400);
    }

    private void hybridVibrate() {
        vibe.vibrate(100);
    }

    private void doneVibrate() {
        vibe.vibrate(50);
    }

    private void call(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                this.startActivity(intent);
            }
        } else {
            this.startActivity(intent);
        }
    }

    private void action(String response) {

        if ((response.contains("kya") || response.contains("what")) && (response.contains("samay") || response.contains("time"))) {
            SimpleDateFormat df = new SimpleDateFormat("hh:mm", Locale.US);
            String time = df.format(new Date());
//            speak("It's " + time + " now");
            speak("अभी " + time + " हो रहा है");
            Toast.makeText(getApplicationContext(), time, Toast.LENGTH_SHORT).show();
            doneVibrate();
        } else if ((response.contains("chalu") || response.contains("on")) && response.contains("hotspot")) {
            if (toggleHotspot(true)) {
                String msg = "Hotspot is Turned On";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                speak("hotspot on कर दी गई है");
            } else {
                String msg = "Hotspot is Already On";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                speak("hotspot पहले से ही on है");
            }
        } else if ((response.contains("band") || response.contains("off")) && response.contains("hotspot")) {
            if (toggleHotspot(false)) {
                String msg = "Hotspot is Turned Off";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                speak("hotspot बंद कर दी गई है");
            } else {
                String msg = "Hotspot is Already Off";
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                speak("hotspot पहले से ही बंद है");
            }
        } else if ((response.contains(" ko ") || response.contains(" to ") || response.contains(" on ") || response.contains(" per ") || response.contains(" par ")) && response.contains("call")) {
            Log.d("tagtag", "start getting contacts");
            String name = null;
            if (response.contains(" to ") || response.contains(" on ")) {
                String[] base;
                if (response.contains(" on "))
                    base = response.split(" on ");
                else
                    base = response.split(" to ");
                if (base.length > 1) {
                    name = base[1].trim();
                    Log.d("tagtag", "'" + name + "'");
                }
            } else {
                String[] base;
                if (response.contains(" ko "))
                    base = response.split(" ko ");
                else if (response.contains(" per "))
                    base = response.split(" per ");
                else
                    base = response.split(" par ");
                if (base.length > 1) {
                    name = base[0].trim();
                    Log.d("tagtag", "'" + name + "'");
                }
            }
            if (name == null) {
                Toast.makeText(this, "I can't hear properly", Toast.LENGTH_SHORT).show();
                speak("Sorry, मैंने ठीक से सुना नहीं");
                hybridVibrate();
            } else if (name.trim().isEmpty()) {
                Toast.makeText(this, "I can't hear properly", Toast.LENGTH_SHORT).show();
                speak("Sorry, मैंने ठीक से सुना नहीं");
                hybridVibrate();
            } else {
                String regexStr = "^[0-9\\s]*$";
                if (name.matches(regexStr)) {
                    call(name);
                    return;
                }
                ArrayList<ContactsModel> contacts = getContactList(name);
                Log.d("tagtag", "all contacts getted");
                if (contacts == null) {
                    hybridVibrate();
                    Toast.makeText(this, "I can't hear properly", Toast.LENGTH_SHORT).show();
                    speak("Sorry, मैंने ठीक से सुना नहीं");
                } else if (contacts.size() > 0) {
                    ContactsModel first = null;
                    for (ContactsModel contactsModel : contacts) {
                        if (first == null)
                            if (name.equals(contactsModel.getName()))
                                first = contactsModel;
                    }
                    if (first == null)
                        first = contacts.get(0);
                    Log.d("tagtag", "\nready to go");
                    call(first.getMobile());
                } else {
                    Toast.makeText(this, "No contact found", Toast.LENGTH_SHORT).show();
                    speak("मुझे कोई contact नहीं मिला");
                }
            }
        } else {
            errorVibrate();
            Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean toggleHotspot(boolean toggle) {
        if (toggle) {
            if (ApManager.isApOn(getApplicationContext())) {
                return false;
            } else {
                ApManager.configApState(getApplicationContext());
                return true;
            }
        } else {
            if (ApManager.isApOn(getApplicationContext())) {
                ApManager.configApState(getApplicationContext());
                return true;
            } else {
                return false;
            }
        }
    }

    private ArrayList<ContactsModel> getContactList(String res) {
        if (res == null) {
            return null;
        }
        res = res.trim();
        if (res.isEmpty()) {
            return null;
        }
        String query;
        if (res.contains(" ")) {
            StringBuilder builder = new StringBuilder();
            String[] base = res.split(" ");
            for (String newB : base) {
                builder.append(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                builder.append(" like '%").append(newB).append("%' AND ");
            }
            String builderSt = builder.toString();
            query = builderSt.substring(0, builderSt.length() - 5);
        } else {
            query = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like '%" + res + "%'";
        }
        Log.d("tagtag", "query: " + query);
        ContentResolver cr = getContentResolver();
        ArrayList<ContactsModel> contacts = new ArrayList<>();
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, query, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {

            while (cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    if (pCur != null) {
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                            Log.d("tagtag", "name: " + name + " : " + phoneNo);

                            boolean duplicate = false;
                            for (ContactsModel contactsModel : contacts) {
                                if (parseContact(phoneNo).equals(parseContact(contactsModel.getMobile()))) {
                                    duplicate = true;
                                }
                            }
                            if (!duplicate)
                                contacts.add(new ContactsModel(name.toLowerCase(), phoneNo));
                        }
                        pCur.close();
                    }
                }
            }
        }
        if (cur != null) {
            cur.close();
        }
        return contacts;
    }

    private String parseContact(String phone) {
        String p = phone.replaceAll(" ", "")
                .replaceAll("-", "");
        if (p.length() > 10) {
            return p.substring(p.length() - 10);
        }
        return p;
    }
}
