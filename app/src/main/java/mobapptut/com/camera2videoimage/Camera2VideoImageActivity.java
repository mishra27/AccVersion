package mobapptut.com.camera2videoimage;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Camera2VideoImageActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "Camera2VideoImageActivi";

    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT = 1;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private int mCaptureState = STATE_PREVIEW;
    private TextureView mTextureView;
    boolean startAcc = false;
    ArrayList<Double> accX = new ArrayList<Double>();

    ArrayList<Double> accXfilter = new ArrayList<Double>();
    ArrayList<Double> velXfilter = new ArrayList<Double>();
    ArrayList<Double> disXfilter = new ArrayList<Double>();

    ArrayList<Double> accY= new ArrayList<Double>();

    ArrayList<Double> accYfilter = new ArrayList<Double>();
    ArrayList<Double> velYfilter = new ArrayList<Double>();
    ArrayList<Double> disYfilter = new ArrayList<Double>();

    ArrayList<Double> accZ = new ArrayList<Double>();
    ArrayList<Double> accZfilter = new ArrayList<Double>();
    ArrayList<Double> velZfilter = new ArrayList<Double>();
    ArrayList<Double> disZfilter = new ArrayList<Double>();

    ArrayList<Float> timeDiff = new ArrayList<Float>();

    private SensorManager sen;
    Sensor acc;


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            mMediaRecorder = new MediaRecorder();
            if(mIsRecording) {
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startRecord();
                mMediaRecorder.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mChronometer.setBase(SystemClock.elapsedRealtime());
                        mChronometer.setVisibility(View.VISIBLE);
                        mChronometer.start();
                    }
                });
            } else {
                startPreview();
            }
            // Toast.makeText(getApplicationContext(),
            //         "Camera connection made!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private String mCameraId;
    private Size mPreviewSize;
    private Size mVideoSize;
    private Size mImageSize;
    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                }
            };

    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestampt;
    private TextView textView;
    private TextView tv_result;



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


//
//        Log.d(TAG, "timeStamp: " + dT);
//        timeDiff.add( (double)(System.currentTimeMillis()-old)/1000);
//        Log.d(TAG, "timeDiff: " + (double)(System.currentTimeMillis()-old)/1000);
//
//        old = System.currentTimeMillis();


        if(startAcc == true){
            final float dT = (sensorEvent.timestamp - timestampt) * NS2S;
            timestampt = sensorEvent.timestamp;

            timeDiff.add( (dT));
            double x = sensorEvent.values[0];
            double y = sensorEvent.values[1];
            double z = sensorEvent.values[2];

            accX.add(x);
            accY.add(y);
            accZ.add(z);


        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {


    }

    private class ImageSaver implements Runnable {

        private final Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(mImageFileName);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();

                Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mImageFileName)));
                sendBroadcast(mediaStoreUpdateIntent);

                if(fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
    private MediaRecorder mMediaRecorder;
    private Chronometer mChronometer;
    private int mTotalRotation;
    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {

                private void process(CaptureResult captureResult) {
                    switch (mCaptureState) {
                        case STATE_PREVIEW:
                            // Do nothing
                            break;
                        case STATE_WAIT_LOCK:
                            mCaptureState = STATE_PREVIEW;
                            Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                            if(afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                    afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                Toast.makeText(getApplicationContext(), "AF Locked!", Toast.LENGTH_SHORT).show();
                                startStillCaptureRequest();
                            }
                            break;
                    }
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    process(result);
                }
            };
    private CameraCaptureSession mRecordCaptureSession;
    private CameraCaptureSession.CaptureCallback mRecordCaptureCallback = new
            CameraCaptureSession.CaptureCallback() {

                private void process(CaptureResult captureResult) {
                    switch (mCaptureState) {
                        case STATE_PREVIEW:
                            // Do nothing
                            break;
                        case STATE_WAIT_LOCK:
                            mCaptureState = STATE_PREVIEW;
                            Integer afState = captureResult.get(CaptureResult.CONTROL_AF_STATE);
                            if(afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                    afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                Toast.makeText(getApplicationContext(), "AF Locked!", Toast.LENGTH_SHORT).show();
                                startStillCaptureRequest();
                            }
                            break;
                    }
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    process(result);
                }
            };
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private ImageButton mRecordImageButton;
    private ImageButton mStillImageButton;
    private boolean mIsRecording = false;
    private boolean mIsTimelapse = false;

    private File mVideoFolder;
    private String mVideoFileName;
    private File mImageFolder;
    private String mImageFileName;

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum( (long)(lhs.getWidth() * lhs.getHeight()) -
                    (long)(rhs.getWidth() * rhs.getHeight()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2_video_image);

        createVideoFolder();
        createImageFolder();

        sen = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        acc = sen.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if(acc != null)
            sen.registerListener(Camera2VideoImageActivity.this, acc, 40000);
       // mSensorManagerAccelero.registerListener (this, mAcceleroSensor, 1000000, 1000000)


        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mStillImageButton = (ImageButton) findViewById(R.id.cameraImageButton2);
        mStillImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!(mIsTimelapse || mIsRecording)) {
                    checkWriteStoragePermission();
                }
                lockFocus();
            }
        });


        mRecordImageButton = (ImageButton) findViewById(R.id.videoOnlineImageButton);
        mRecordImageButton.setOnClickListener(new View.OnClickListener() {
            PrintWriter writer = null;
            @Override

            public void onClick(View v) {
                if (mIsRecording || mIsTimelapse) {
                    mChronometer.stop();
                    mChronometer.setVisibility(View.INVISIBLE);
                    mIsRecording = false;
                    mIsTimelapse = false;
                    mRecordImageButton.setImageResource(R.mipmap.btn_video_online);

                    // Starting the preview prior to stopping recording which should hopefully
                    // resolve issues being seen in Samsung devices.
                    startPreview();
                    mMediaRecorder.stop();
                    startAcc = false;
                    mMediaRecorder.reset();
                    double sumT = 0;

                    float aT = timestampt;
                    String filePath = Environment.getExternalStorageDirectory().getPath()+"/AccData/" + aT  +  ".txt";
                    try {
                        writer = new PrintWriter(filePath);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    for(int mk = 1; mk < accX.size(); mk++) {
                        Log.d("dd", "AccX: " + (accX.get(mk)) + " Time " + timeDiff.get(mk) + " Index " + mk );
                        sumT = sumT + timeDiff.get(mk);
                        writer.println(accX.get(mk) + "," + sumT);
                    }

                    Log.d("dd", "toatl Time: " + sumT);
                    Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mVideoFileName)));
                    sendBroadcast(mediaStoreUpdateIntent);
                    writer.close();

                    double K =0;

//                    for(int i = timeDiff.size()-1; i > 0; i--) {
//
//                        timeDiff.set(i, timeDiff.get(i) - timeDiff.get(i-1));
//                    }
                    for(int i =0; i<accX.size();i++) {
                        accXfilter.add(accX.get(i));
                    }

                    for(int i =0; i<accY.size();i++) {
                        accYfilter.add(accY.get(i));
                    }


                    int n = accX.size();


                    // % corrected state vectors
                    double[] z = matrix(0f, n);
                    // z =

                    double[] x = matrix(0f, n);
                    //% corrected error co-variance matrix

                    double[] p = matrix(1, n); //one    P = ones(n,1);
                    //% predicted state vectors

                    double[] x_p = matrix(0f, n);//            x_p = zeros(n,1);
                    //% predicted error co-variance matrix

                    double[] p_p = matrix(0f, n);   // P_p = zeros(n,1);
                    p_p[0] =2;
                    //% variance of sensor noise
                    //double r = 0.000625;

                    Log.d("dd ", "AccX SIZE: " + ((accX.size())));
                    Log.d("dd ", "AccX filter SIZE: " + ((accXfilter.size())));


                    ArrayList<Double> sub1 = new ArrayList<Double>(accXfilter.subList(0,50));
                    // ArrayList<Double> sub = new ArrayList<Double>(accXfilter.subList(j, j + 25));




                    double sd1 = calculateSD(sub1);
                    double r =sd1*sd1;
                    Log.d("asdasdasdsadasdasdad", "standard DAVIATOIn" + sd1);
                    double Q = 0.0005   ;
                    for(int k =0; k <accXfilter.size()-1; k++){
                        //% prediction
                        x_p[k+1]= x[k];
                        p_p[k+1] = p[k];

                        //% correction
                        K = p_p[k+1]/(p_p[k+1] + r);
                        x[k+1] = x_p[k+1] + K*(accXfilter.get(k+1) - x_p[k+1]);
                        p[k+1] = (1 - K)* p_p[k+1] + Q;


                        if(accXfilter.get(k+1) != 0 )
                            accXfilter.set(k+1, x[k+1]);
                        // Log.d(TAG, "Kalman: " + x[k+1] );
                    }

                    z = matrix(0f, n);
                    // z =

                    x = matrix(0f, n);
                    //% corrected error co-variance matrix

                    p = matrix(1, n); //one    P = ones(n,1);
                    //% predicted state vectors

                    x_p = matrix(0f, n);//            x_p = zeros(n,1);
                    //% predicted error co-variance matrix

                    p_p = matrix(0f, n);   // P_p = zeros(n,1);
                    p_p[0] =2;
                    //% variance of sensor noise
                    //double r = 0.000625;

                    ArrayList<Double> sub3 = new ArrayList<Double>(accYfilter.subList(0,50));
                    double sdY = calculateSD(sub3);
                    double rY =sd1*sd1;
                    Log.d("asdasdasdsadasdasdad", "standard DAVIATOIn" + sdY);
                    double QY = 0.0005   ;
                    for(int k =0; k <accYfilter.size()-1; k++){
                        //% prediction
                        x_p[k+1]= x[k];
                        p_p[k+1] = p[k];

                        //% correction
                        K = p_p[k+1]/(p_p[k+1] + rY);
                        x[k+1] = x_p[k+1] + K*(accYfilter.get(k+1) - x_p[k+1]);
                        p[k+1] = (1 - K)* p_p[k+1] + QY;


                        if(accYfilter.get(k+1) != 0 )
                            accYfilter.set(k+1, x[k+1]);
                        // Log.d(TAG, "Kalman: " + x[k+1] );
                    }






                    ArrayList<Double> sub2 = new ArrayList<Double>(accXfilter.subList(0,50));
                    // ArrayList<Double> sub = new ArrayList<Double>(accXfilter.subList(j, j + 25));
                    double sum1 = 0;
                    for(int i = 0; i<sub2.size(); i++){
                        sum1 += sub2.get(i);
                    }

                    double mean1 = (sum1/sub2.size());

                    for(int i = 0; i < accXfilter.size(); i++){
                        if(mean1 <0)
                            accXfilter.set(i, accXfilter.get(i)-mean1);

                        else
                            accXfilter.set(i, accXfilter.get(i)-mean1);

                    }
                    int j=0;
                    double n1 = 0.05;
                    double m = 0.1;
                    // int k = 0;

                    while( j < accXfilter.size()){
                        if(j+10<accXfilter.size()) {
                            int temp = j;
                            ArrayList<Double> sub = new ArrayList<Double>(accXfilter.subList(j, j + 10));
                            double sd = calculateSD(sub);



                            double sum = 0.0;
                            // standardDeviation = 0.0;
                            //  double max = 0;
                            for(int i = 0; i<10; i++){
                                sum += sub.get(i);
                                // if(sub.get(i)>max){
                                //     max = sub.get(i);
                                // }
                            }

                            double mean = Math.abs(sum/sub.size());

                            if( sd < n1 && mean < m){
                                for(int i = j ; i<j+10; i++){
                                    accXfilter.set(i, 0.0);


                                }

                                int a = j -1;
                                int pop = 0;

                                if(a>=0) {
                                    while (accX.get(a) > 0 && a>0) {
                                        accXfilter.set(a, 0.0);
                                        a--;
                                        pop++;
                                    }


                                }



                                Log.d("askdakdjak ", "standard D " + sd + " Mean " + mean + " repeats: " + pop);

                            }




                        }
                        else{
                            ArrayList<Double> sub = new ArrayList<Double>(accXfilter.subList(j, accXfilter.size()));
                            double sd = calculateSD(sub);

                            double sum = 0.0;
                            // standardDeviation = 0.0;

                            for(int i = 0; i<sub.size(); i++){
                                sum += sub.get(i);
                            }

                            double mean = sum/sub.size();

                            if(sd >= n1){}
                            else if( sd < n1 && mean < m){
                                for(int i = j ; i<accXfilter.size(); i++){
                                    accXfilter.set(i, 0.0);
                                }
                            }
                        }


                        j += 10;
                    }

                    int signChange = 0;
//                    for(int i = 50; i < accXfilter.size()-1; i++){
//                        if( signChange < 1 && accXfilter.get(i)*accXfilter.get(i+1)<0 && accXfilter.get(i+10)<0 ){
//                            signChange++;
//                        }
//
//                        else if(accXfilter.get(i)*accXfilter.get(i+1)<0 && accXfilter.get(i-10)<0 && signChange < 2){
//                            signChange++;
//                        }
//
//                        if(signChange >= 2){
//                            accXfilter.set(i, 0.0);
//                        }
//
//
//                    }


//            ArrayList<Double> sub2Y = new ArrayList<Double>(accYfilter.subList(0,50));
//            // ArrayList<Double> sub = new ArrayList<Double>(accXfilter.subList(j, j + 25));
//            double sum1Y = 0;
//            for(int i = 0; i<sub2Y.size(); i++){
//                sum1Y += sub2Y.get(i);
//            }
//
//            double mean1Y = (sum1Y/sub2Y.size());
//
//            for(int i = 0; i < accYfilter.size(); i++){
//                if(mean1Y <0)
//                    accYfilter.set(i, accYfilter.get(i)-mean1Y);
//
//                else
//                    accYfilter.set(i, accYfilter.get(i)-mean1Y);
//
//            }
//            int jY=0;
//            double n1Y = 0.05;
//            double mY = 0.1;
//            // int kY = 0;
//
//            while( jY < accYfilter.size()){
//                if(jY+10<accYfilter.size()) {
//                    int tempY = jY;
//                    ArrayList<Double> subY = new ArrayList<Double>(accYfilter.subList(jY, jY + 10));
//                    double sd1Y = calculateSD(subY);
//
//
//
//                    double sumY = 0.0;
//                    // standardDeviation = 0.0;
//                    //  double max = 0;
//                    for(int i = 0; i<10; i++){
//                        sumY += subY.get(i);
//                        // if(sub.get(i)>max){
//                        //     max = sub.get(i);
//                        // }
//                    }
//
//                    double meanY = Math.abs(sumY/subY.size());
//
//                    if( sd1Y < sdY && meanY < mY){
//                        for(int i = jY ; i<jY+10; i++){
//                            accYfilter.set(i, 0.0);
//
//
//                        }
//
//                        int aY = jY -1;
//                        //int popY = 0;
//
//                        if(aY>=0) {
//                            while (accY.get(aY) < 0 && aY>
//                                    0) {
//                                accYfilter.set(aY, 0.0);
//                                aY--;
//                                //pop++;
//                            }
//
//
//                        }
//
//
//
//                        //Log.d("askdakdjak ", "standard D " + sd + " Mean " + mean + " repeats: " + pop);
//
//                    }
//
//
//
//
//                }
//                else{
//                    ArrayList<Double> subY = new ArrayList<Double>(accYfilter.subList(jY, accYfilter.size()));
//                    double sd1Y = calculateSD(subY);
//
//                    double sumY = 0.0;
//                    // standardDeviation = 0.0;
//
//                    for(int i = 0; i<subY.size(); i++){
//                        sumY += subY.get(i);
//                    }
//
//                    double meanY = sumY/subY.size();
//
//                    if(sd1Y >= n1Y){}
//                    else if( sdY < n1Y && meanY < mY){
//                        for(int i = jY ; i<accYfilter.size(); i++){
//                            accYfilter.set(i, 0.0);
//                        }
//                    }
//                }
//
//
//                jY += 10;
//            }

                    calculation(accX);
                    // calculation(accY);

                    tv_result = (TextView) findViewById(R.id.tv_result);

                    tv_result.setText("The distance is: " + String.format( "%.2f", (disXfilter.get(disXfilter.size()-1))*100) + " cm");





                } else {
                    mIsRecording = true;


                    mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                    checkWriteStoragePermission();
                    startAcc = true;

                }
            }
        });
        mRecordImageButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mIsTimelapse =true;
                mRecordImageButton.setImageResource(R.mipmap.btn_timelapse);
                checkWriteStoragePermission();
                return true;
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if(mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not run without camera services", Toast.LENGTH_SHORT).show();
            }
            if(grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not have audio on record", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(mIsRecording || mIsTimelapse) {
                    mIsRecording = true;
                    mRecordImageButton.setImageResource(R.mipmap.btn_video_busy);
                }
                Toast.makeText(this,
                        "Permission successfully granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "App needs to save video to run", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        closeCamera();

        stopBackgroundThread();

        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
                        Toast.makeText(this,
                                "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {android.Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
                    }, REQUEST_CAMERA_PERMISSION_RESULT);
                }

            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {

        try {
            if(mIsRecording) {
                setupMediaRecorder();
            } else if(mIsTimelapse) {
                setupTimelapse();
            }
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mRecordCaptureSession = session;
                            try {
                                mRecordCaptureSession.setRepeatingRequest(
                                        mCaptureRequestBuilder.build(), null, null
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                            //startAcc = true;

                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startRecord");
                            startAcc = false;

                        }
                    }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigured: startPreview");
                            mPreviewCaptureSession = session;
                            try {
                                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startPreview");

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStillCaptureRequest() {
        try {
            if(mIsRecording) {
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            } else {
                mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);

            CameraCaptureSession.CaptureCallback stillCaptureCallback = new
                    CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                            super.onCaptureStarted(session, request, timestamp, frameNumber);

                            try {
                                createImageFileName();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };

            if(mIsRecording) {
                mRecordCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
            } else {
                mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if(mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices) {
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private void createVideoFolder() {
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        mVideoFolder = new File(movieFile, "Videos");
        if(!mVideoFolder.exists()) {
            mVideoFolder.mkdirs();
        }
    }

    private File createVideoFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "VIDEO_" + timestamp + "_";
        File videoFile = File.createTempFile(prepend, ".mp4", mVideoFolder);
        mVideoFileName = videoFile.getAbsolutePath();


        return videoFile;
    }

    private void createImageFolder() {
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder = new File(imageFile, "camera2VideoImage");
        if(!mImageFolder.exists()) {
            mImageFolder.mkdirs();
        }
    }

    private File createImageFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void checkWriteStoragePermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    createVideoFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(mIsTimelapse || mIsRecording) {
                    startRecord();
                    mMediaRecorder.start();
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.setVisibility(View.VISIBLE);
                    mChronometer.start();
                }
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "app needs to be able to save videos", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
            }
        } else {
            try {
                createVideoFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(mIsRecording || mIsTimelapse) {
                startRecord();
                mMediaRecorder.start();
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.setVisibility(View.VISIBLE);
                mChronometer.start();
            }
        }
    }


    private void setupMediaRecorder() throws IOException {
//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(15000000);
        mMediaRecorder.setVideoFrameRate(25);
        mMediaRecorder.setVideoSize(1280 , 720);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();
    }

    private void setupTimelapse() throws IOException {
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setCaptureRate(2);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();
    }

    private void lockFocus() {
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            if(mIsRecording) {
                mRecordCaptureSession.capture(mCaptureRequestBuilder.build(), mRecordCaptureCallback, mBackgroundHandler);
            } else {
                mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void calculation (ArrayList<Double> x){


        velXfilter.add(0.0);
        //double max = 0;
        int maxi = 0;
        timeDiff.set(1, 0.0f);
        for(int i = 1; i < accX.size(); i++){

            if(accXfilter.get(i) != 0) {
                double val = velXfilter.get(i - 1) + (((accXfilter.get(i) + accXfilter.get(i-1))/2.0) * timeDiff.get(i));
                velXfilter.add(val);
            }

            else
                velXfilter.add(0.0);

//            double val = velXfilter.get(i - 1) + (((accXfilter.get(i))) * timeDiff.get(1));
//            velXfilter.add(val);

        }
        int count = 0;

        for(int i = 50; i <velXfilter.size()-1; i++){
            double val =  velXfilter.get(i);
            if(val > velXfilter.get(i+1) && val > velXfilter.get(i-1)) {
                maxi = maxi + i;
                count++;

                Log.d("tag", "MAXi " + i);

            }
        }

        if(count != 0)
            maxi = maxi/count;

        Log.d("tag", "MAXIIIII " + maxi);
        double nv = 0.0001;
        int jv = 0;

        while( jv < velXfilter.size()){

            if(jv+10<velXfilter.size()) {
                ArrayList<Double> sub = new ArrayList<Double>(velXfilter.subList(jv, jv + 10));
                double sd = calculateSD(sub);

//                double temp = 0;


                if(sd >= nv){}
                else if( sd < nv){
                    for(int i = jv ; i<jv+10; i++){
                        velXfilter.set(i, 0.0);
                    }
                }
            }


            else{
                ArrayList<Double> sub = new ArrayList<Double>(velXfilter.subList(jv, velXfilter.size()));
                double sd = calculateSD(sub);


                if(sd >= nv){}
                else if( sd < nv ){
                    for(int i = jv ; i<velXfilter.size(); i++){
                        velXfilter.set(i, 0.0);
                    }
                }
            }


            jv += 10;
        }

        double temp;
        disXfilter.add(0.0);
        for(int i = 1; i < velXfilter.size(); i++){
            temp = disXfilter.get(i-1);
            double val =  disXfilter.get(i-1)+(((velXfilter.get(i) + velXfilter.get(i-1))/2.0)*timeDiff.get(i-1));

            if(temp<val)
                disXfilter.add(val);

            else
                disXfilter.add(temp);

        }



        velYfilter.add(0.0);
        //double max = 0;
        int maxiY = 0;
        for(int i = 1; i < accY.size(); i++){

//            if(accXfilter.get(i) != 0) {
//                double val = velXfilter.get(i - 1) + (((accXfilter.get(i))) * timeDiff.get(1));
//                velXfilter.add(val);
//            }
//
//            else
//                velXfilter.add(0.0);

            double val = velYfilter.get(i - 1) + (((accYfilter.get(i))) * timeDiff.get(1));
            velYfilter.add(val);

        }
        int countY = 0;

        for(int i = 50; i <velYfilter.size()-1; i++){
            double val =  velYfilter.get(i);
            if(val > velYfilter.get(i+1) && val > velYfilter.get(i-1)) {
                maxiY = maxiY + i;
                countY++;

                Log.d("tag", "MAXiY " + i);

            }
        }

        // maxiY = maxiY/countY;

        Log.d("tag", "MAXIIIII " + maxiY);
        double nvY = 0.0001;
        int jvY = 0;

        while( jvY < velYfilter.size()){

            if(jvY+10<velYfilter.size()) {
                ArrayList<Double> subY = new ArrayList<Double>(velYfilter.subList(jvY, jvY + 10));
                double sdY = calculateSD(subY);

//                double tempY = 0;


                if(sdY >= nvY){}
                else if( sdY < nvY){
                    for(int i = jvY ; i<jvY+10; i++){
                        velYfilter.set(i, 0.0);
                    }
                }
            }


            else{
                ArrayList<Double> subY = new ArrayList<Double>(velYfilter.subList(jvY, velYfilter.size()));
                double sdY = calculateSD(subY);


                if(sdY >= nvY){}
                else if( sdY < nvY ){
                    for(int i = jvY ; i<velYfilter.size(); i++){
                        velYfilter.set(i, 0.0);
                    }
                }
            }


            jvY += 10;
        }

        double tempY;
        disYfilter.add(0.0);
        for(int i = 1; i < velYfilter.size(); i++){
            tempY = disYfilter.get(i-1);
            double valY =  disYfilter.get(i-1)+(((velYfilter.get(i)))*timeDiff.get(i-1));

            if(tempY>valY)
                disYfilter.add(valY);

            else
                disYfilter.add(tempY);

        }

        if(disYfilter.get(disYfilter.size()-1)>0.02){
            disYfilter.set(disYfilter.size()-1,disYfilter.get(disYfilter.size()-1));
        }

        else if(disYfilter.get(disYfilter.size()-1)< -0.02){
            disYfilter.set(disYfilter.size()-1,disYfilter.get(disYfilter.size()-1));
        }


        if(disXfilter.get(disXfilter.size()-1)>0.02){
            disXfilter.set(disXfilter.size()-1,disXfilter.get(disXfilter.size()-1));
        }

        else if(disXfilter.get(disXfilter.size()-1)< -0.02){
            disXfilter.set(disXfilter.size()-1,disXfilter.get(disXfilter.size()-1));
        }


        for(int mk = 2; mk < accX.size()-1; mk++) {
            Log.d("dd", "AccX: " + (accX.get(mk))  + "AccX Filtered: " + (accXfilter.get(mk)) + " vel " + (velXfilter.get(mk-1))+  " dis " + (disXfilter.get(mk-2)) + " index " + mk + " Time " + timeDiff.get(mk-1) );

        }
        for(int mk = 2; mk < accX.size()-1; mk++)
            Log.d("dd", "AccY: " + (accY.get(mk))  + "AccY Filtered: " + (accYfilter.get(mk)) + " vel " + (velYfilter.get(mk-1))+  " dis " + (disYfilter.get(mk-2)) + " index " + mk + " Time " + timeDiff.get(mk-1) );

        Log.d("dd ", "E_AccX: " + (accX.get(accX.size()-1)) + " E_VelX: " + (velXfilter.get(velXfilter.size()-1)) + " E_DisX: " + (disXfilter.get(disXfilter.size()-1)));

        Double xD =  disXfilter.get(disXfilter.size()-1);
        Double yD = disYfilter.get(disYfilter.size()-1);
      //  textView = (TextView) findViewById(R.id.textView);
      //camer  float answer = (float)((2*disXfilter.get(maxi-1) )) ;
      //camer  float answer = (float)((2*disXfilter.get(maxi-1) )) ;

        // Double answer = Math.sqrt((xD*xD)+(yD*yD));

        // Double answer = yD;
      //  textView.setText(String.format( "%.2f", (answer)*100)+ " cm");
        // tv_result.setText("The distance is: " + String.format( "%.2f", (answer) + " cm");


        //textView.setText("");

    }

    public static double calculateSD(ArrayList<Double> x)
    {
        double sum = 0.0, standardDeviation = 0.0;

        for(int i = 0; i< x.size(); i++) {
            sum += x.get(i);
        }

        double mean = sum/x.size();

        for(int i = 0; i< x.size(); i++) {
            standardDeviation += Math.pow(x.get(i) - mean, 2);
        }

        return Math.sqrt(standardDeviation/x.size());
    }

    private static double[] matrix( double a, int n){
        double[] array = new double[n];
        for (int x = 0; x < array.length; x++) {

            array[x] = a;

        }
        return array;
    }

}
