package com.toprako.toprakoocr;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.support.design.widget.Snackbar;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.toprako.toprakoocr.camera.CameraSource;
import com.toprako.toprakoocr.camera.CameraSourcePreview;
import com.toprako.toprakoocr.camera.GraphicOverlay;
import com.toprako.toprakoocr.OcrGraphic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class OcrCaptureActivity extends AppCompatActivity {
    private static final String TAG = "OcrActivity";
    private static final int Handl_Gms = 9001;//olasi hata mesaj

    private static final int Camera_izin = 2;
    public static final String focus = "OtomatikFocus";
    public static final String flash = "FlashKullan";
    public static final String geriText = "String";
    public static final String geriResim = "Image";
    private CameraSource cameraSource;
    private OcrGraphic ocrGraphic;
    private CameraSourcePreview cameraSourcePreview;
    private GraphicOverlay<OcrGraphic> ocrGraphicGraphicOverlay;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private Bitmap Resim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_capture);

        cameraSourcePreview = (CameraSourcePreview) findViewById(R.id.preview);
        ocrGraphicGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);

        boolean otomatikfocus = getIntent().getBooleanExtra(focus, false);
        boolean flashkullanma = getIntent().getBooleanExtra(flash, false);

        int izinvar = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (izinvar == PackageManager.PERMISSION_GRANTED) {
            KameraKaynakOlustur(otomatikfocus, flashkullanma);
        } else {
            iziniste();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        Snackbar.make(ocrGraphicGraphicOverlay, "Yakalamak için dokunun. Yakınlaştırmak için iki parmak ile Sıkıştır / Uzaklaştır ", Snackbar.LENGTH_LONG).show();

    }

    private void iziniste() {
        final String[] izinler = new String[]{Manifest.permission.CAMERA};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, izinler, Camera_izin);
            return;
        }
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(OcrCaptureActivity.this, izinler, Camera_izin);
            }
        };

        Snackbar.make(ocrGraphicGraphicOverlay, "Algılama için kameraya erişim gerekiyor",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("Tamam", listener)
                .show();
    }

    //Tıklanma Olaylarını Tanımlaadık.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean b = scaleGestureDetector.onTouchEvent(event);
        boolean c = gestureDetector.onTouchEvent(event);
        return b || c || super.onTouchEvent(event);
    }

    private void KameraKaynakOlustur(boolean focus, Boolean flash) {
        Context context = getApplicationContext();

        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(ocrGraphicGraphicOverlay));
        //kutuphane kontrol
        if (!textRecognizer.isOperational()) {
            IntentFilter dusukDepolamaFiltre = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean dusukDepolamami = registerReceiver(null, dusukDepolamaFiltre) != null;

            if (dusukDepolamami) {
                Toast.makeText(getApplicationContext(), "Düşük Cihaz Depolama Hatası", Toast.LENGTH_LONG).show();
            }
        }

        cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setRequestedFps(10.0f)
                .setFlashMode(flash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .setFocusMode(focus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                .build();

    }

    //kamerayi tekrar baslatma
    @Override
    protected void onResume() {
        super.onResume();
        BaslatKameraKaynak();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSourcePreview != null)
            cameraSourcePreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSourcePreview != null)
            cameraSourcePreview.release();
    }

    //izin geri dönüş
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != Camera_izin) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if(grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            boolean focus_b = getIntent().getBooleanExtra(focus,false);
            boolean flash_b = getIntent().getBooleanExtra(flash,false);
            KameraKaynakOlustur(focus_b,flash_b);
            return;
        }

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("İzinler")
        .setMessage("Kamera izni olmadığı için bu uygulama çalıştırılamıyor. İzin Verilmedi.\n")
        .setPositiveButton("Tamam",listener)
        .show();
    }

    private void BaslatKameraKaynak() throws SecurityException{
        int kod = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (kod != ConnectionResult.SUCCESS){
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this,kod,Handl_Gms);
            dialog.show();
        }

        if (cameraSource != null){
            try {
                cameraSourcePreview.start(cameraSource,ocrGraphicGraphicOverlay);
            }catch (IOException e){
                cameraSource.release();
                cameraSource = null;
            }
        }
    }
    //Cıkan Yazıda tıklama
    private boolean onTap(float satirX , Float satirY){
        OcrGraphic graphic = ocrGraphicGraphicOverlay.getGraphicAtLocation(satirX,satirY);
        TextBlock yazi = null;
        if (graphic != null){
            yazi = graphic.getTextBlock();
            if (yazi != null && yazi.getValue() != null){
                Intent veri = new Intent();
                veri.putExtra(geriText,yazi.getValue());
                setResult(CommonStatusCodes.SUCCESS,veri);
                finish();
            }
        }
        return  yazi != null;
    }

    //tıklanma ayarını aldık
    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return  onTap(e.getRawX() , e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener{

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            cameraSource.doZoom(detector.getScaleFactor());
        }
    }


}
