package com.shubham16.aiproject;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AssistantActivity extends AppCompatActivity {

    ImageButton micButton;

    private android.speech.SpeechRecognizer androidRecognizer;
    private Intent recognizerIntent;
    private AudioManager audioManager;
    private Vibrator vibe;
    private TextToSpeech myTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);

        micButton = findViewById(R.id.micButton);
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startListening();
            }
        });

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initializeTTS();
        setRecogniserIntent();
        androidRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
        setupAndroidRecognizer();
    }

    private void startListening() {
        if (vibe != null) {
            vibe.vibrate(200);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                androidRecognizer.startListening(recognizerIntent);
            }
        }, 200);
    }

    private void initializeTTS() {
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (myTTS.getEngines().size() == 0) {
                    Toast.makeText(AssistantActivity.this, "There is no TTS", Toast.LENGTH_SHORT).show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        myTTS.setLanguage(Locale.forLanguageTag("en-in"));
                        myTTS.setVoice(new Voice("en-in-x-ahp#male_3-local", Locale.forLanguageTag("en-in"), Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, null));
                    }
                    speak("          " + "Hello Sir, मै आप के लिए क्या कर सकता हूँ");
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
        Toast.makeText(this, "Android Speech Recognizer not found", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myTTS.shutdown();
        if (androidRecognizer != null) {
            androidRecognizer.cancel();
            androidRecognizer.destroy();
        }
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
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                }
            }, 500);
            errorVibrate();
        }

        @Override
        public void onResults(Bundle bundle) {
            final ArrayList<String> arrayList = bundle.getStringArrayList("results_recognition");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                    if (arrayList != null) {
                        action(arrayList.get(0).toLowerCase());
                    }
                }
            }, 500);

        }
    };

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
                // TODO: Consider calling
                //    Activity#requestPermissions
                this.startActivity(intent);
            }
        } else {
            this.startActivity(intent);
        }
    }

    private void action(String response) {
        /*if (response.contains("समय")){
            if (response.contains("क्या") || response.contains("कितना")) {
                Toast.makeText(getApplicationContext(), new Date().toString(), Toast.LENGTH_SHORT).show();
                vibe.vibrate(100);
            }*/
        if (response.contains("time") || response.contains("samay")) {
            if (response.contains("kya") || response.contains("what")) {
                speak("          " + new Date().toString());
                Toast.makeText(getApplicationContext(), new Date().toString(), Toast.LENGTH_SHORT).show();
                doneVibrate();
            }
        } else if (response.contains("on") || response.contains("chalu")) {
            if (response.contains("hotspot")) {
                if (ApManager.isApOn(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "Hotspot is already on", Toast.LENGTH_SHORT).show();
                    speak("hotspot पहले से ही on है");
                    hybridVibrate();
                } else {
                    ApManager.configApState(getApplicationContext());
                    Toast.makeText(getApplicationContext(), "Hotspot is turned on", Toast.LENGTH_SHORT).show();
                    speak("hotspot on कर दी गई है");
                    doneVibrate();
                }
            }
        } else if (response.contains("off") || response.contains("band")) {
            if (response.contains("hotspot")) {
                if (ApManager.isApOn(getApplicationContext())) {
                    ApManager.configApState(getApplicationContext());
                    Toast.makeText(getApplicationContext(), "Hotspot is turned off", Toast.LENGTH_SHORT).show();
                    speak("hotspot off कर दी गई है");
                    doneVibrate();

                } else {
                    Toast.makeText(getApplicationContext(), "Hotspot is already off", Toast.LENGTH_SHORT).show();
                    speak("hotspot पहले से ही off है");
                    hybridVibrate();
                }
            }
        } else if (response.contains("call")) {
            if (response.contains("karo") || response.contains("to")) {
                if (response.contains("ashutosh")) {
                    call("7860877582");
                    speak("calling to ashutosh");
                } else if (response.contains("piyush")) {
                    call("7905582771");
                    speak("calling to piyush");
                } /*else if (response.contains("home") || response.contains("mummy") || response.contains("ghar")){
                    call("8318587748");
                } else if (response.contains("papa")){
                    if (response.contains("office")){
                        call("9452775145");
                    } else {
                        call("9889871300");
                    }
                }*/ else if (response.contains("shubham")) {
                    if (response.contains("home")) {
                        call("9336508099");
                        speak("calling to shubham home");
                    } else if (response.contains("jio")) {
                        call("8318554625");
                        speak("calling to shubham jio");
                    } else {
                        call("9616954124");
                        speak("calling to shubham");
                    }
                }
            }
        } else {
            errorVibrate();
            Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
        }
    }
}