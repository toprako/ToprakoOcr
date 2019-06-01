package com.toprako.toprakoocr;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;


import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpConnection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CompoundButton focus, flash;
    private TextView mesaj, okunandeger, cevrilenyabancidil;
    private String APIKEY = "trnsl.1.1.20190424T203123Z.14a04dbbe1c0c855.dc3180b703e269171fe724140e5be2c6e3bb0dbb";
    private static final int OCR_EK = 9003;
    private static final String TAG = "Main";
    private static String dil = "en-tr";
    private static TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mesaj = (TextView) findViewById(R.id.lbl_durum_msg);
        okunandeger = (TextView) findViewById(R.id.lbl_cevrilen_deger);
        focus = (CompoundButton) findViewById(R.id.chck_otomatikfocus);
        flash = (CompoundButton) findViewById(R.id.chck_flash);
        cevrilenyabancidil = (TextView) findViewById(R.id.lbl_cevrilen_deger_translate);

        findViewById(R.id.btn_metnioku).setOnClickListener(this);
        findViewById(R.id.bt_yandexcevir).setOnClickListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OCR_EK) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    String veri = data.getStringExtra(OcrCaptureActivity.geriText);
                    mesaj.setText("Okuma Başarılı");
                    okunandeger.setText(veri);
                } else {
                    mesaj.setText("Yazı Bulunamadı");
                }
            } else {
                mesaj.setText("Veri Okuma Başarısız " + CommonStatusCodes.getStatusCodeString(resultCode));
            }
        } else if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            Intent i = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            startActivity(i);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_metnioku) {
            Intent intent = new Intent(this, OcrCaptureActivity.class);
            intent.putExtra(OcrCaptureActivity.focus, focus.isChecked());
            intent.putExtra(OcrCaptureActivity.flash, flash.isChecked());
            startActivityForResult(intent, OCR_EK);
        }
        if (v.getId() == R.id.bt_yandexcevir) {
            String veri = okunandeger.getText().toString();

            try {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                veri = URLEncoder.encode(veri, "utf-8");
                String url = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=" + APIKEY + "&text=" + veri + "&lang=" + dil;
                new Translate().execute(url);
            } catch (Exception e) {
                Log.e("Hata", e.toString());
            }

        }
    }

    private class Translate extends AsyncTask<String, Void, String> {
        @Override
        protected void onPostExecute(String s) {
            JsonObject jsonObject = new JsonParser().parse(s).getAsJsonObject();
            final String sonuc = jsonObject.get("text").getAsString();
            cevrilenyabancidil.setText(sonuc);
            textToSpeech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    textToSpeech.setLanguage(new Locale("tr"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        textToSpeech.speak(sonuc, TextToSpeech.QUEUE_ADD, null, null);
                    } else {
                        textToSpeech.speak(sonuc, TextToSpeech.QUEUE_ADD, null);
                    }

                }
            });

        }

        @Override
        protected String doInBackground(String... strings) {
            String urlString = strings[0];
            StringBuilder jsonString = new StringBuilder();
            try {
                URL yandexUrl = new URL(urlString);
                HttpURLConnection httpURLConnection = (HttpURLConnection) yandexUrl.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    jsonString.append(line);
                }
                inputStream.close();
                bufferedReader.close();
                httpURLConnection.disconnect();
            } catch (Exception e) {

            }

            return jsonString.toString();
        }
    }
}
