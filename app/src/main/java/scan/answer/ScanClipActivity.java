package scan.answer;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;

/**
 * 扫描画面
 */
public class ScanClipActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener, Handler.Callback, View.OnClickListener {
    private JavaCameraView opencvView;
    private ImageView imageView;
    private Button btnReset;

    private boolean firstBitmap;
    private Bitmap simpleBitmap;
    Mat src = new Mat();
    Mat gray = new Mat();
    Mat edge = new Mat();
    Mat dst = new Mat();

    private double maxArea;

    private Handler handler;

    //定时器，
    private Timer timer = new Timer();
    int record = 0;
    //设置一个定时器，0到220毫秒
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            record += 10;
            if (record > 220) {
                record = 0;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //显示全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //强制横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_scan_clip);
//        staticLoadCVLibraries();

        opencvView = (JavaCameraView) findViewById(R.id.opencv_view);
        imageView = (ImageView) findViewById(R.id.show_img);
        btnReset = (Button) findViewById(R.id.btn_reset);
        handler = new Handler(this);

        //监听opencv
        opencvView.setCvCameraViewListener(ScanClipActivity.this);
        btnReset.setOnClickListener(this);
        timer.schedule(task, 0, 10);
    }


    @Override
    protected void onResume() {
        super.onResume();
        opencvView.enableView();
    }


//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (opencvView != null)
//            opencvView.disableView();
//
//    }
//
//    public void onResume() {
//        super.onResume();
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this,
//                mLoaderCallback);
//    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }


    @Override
    public Mat onCameraFrame(Mat inputFrame) {

        if (inputFrame != null && record > 180) {
            src = inputFrame.clone();

            blackAndWhite(src);



        }
        return inputFrame;
    }

    /**
     * 图片二值化
     */
    private void blackAndWhite(Mat map) {

        // 将原图像转换为灰度图像
        Imgproc.cvtColor(map, gray, Imgproc.COLOR_BGR2GRAY);
        // 使用 3x3内核来降噪
        Imgproc.blur(gray, edge, new Size(3, 3));
        // 自适应二值化
        Imgproc.adaptiveThreshold(edge, dst, 255, ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 0);
        //轮廓集合
        List<MatOfPoint> contours = new ArrayList<>();
        //获取所有轮廓
        Imgproc.findContours(dst, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.size() > 0)
        {

            MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f tempCurve = new MatOfPoint2f();
            List<MatOfPoint> largest_contours = new ArrayList<>();
            //获取最大轮廓
            for (int idx = 0; idx < contours.size(); idx++) {
                temp_contour = contours.get(idx);

                //计算轮廓面积
                double contourarea = Imgproc.contourArea(temp_contour);

                if (contourarea > maxArea) {
                    MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                    int contourSize = (int) temp_contour.total();
                    MatOfPoint approxContour = new MatOfPoint();
                    Imgproc.approxPolyDP(new_mat, approxCurve, contourSize * 0.05, true);
                    approxCurve.convertTo(approxContour, CvType.CV_32S);

                    //判断是不是矩形
                    if (approxContour.size().height == 4) {
                        maxArea = contourarea;
                        largest_contours.add(temp_contour);
                        tempCurve = approxCurve;
                    }
                }
            }
            if (largest_contours.size() > 0) {
                MatOfPoint temp_largest = largest_contours.get(largest_contours.size() - 1);
                largest_contours = new ArrayList<>();
                largest_contours.add(temp_largest);
                //用矩形把轮廓装起来
                Rect rect = Imgproc.boundingRect(temp_largest);
                //获取最大轮廓的包围矩形来判断是否符合机读卡标准:塑料卡
                if (rect.width > map.width() * 0.44 && rect.height > map.height() * 0.90 && (float) rect.width / rect.height > 0.81 && (float) rect.width / rect.height < 0.86) {


//                        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BayerBG2RGB);

                    //最大轮廓闪现提示
                    Imgproc.drawContours(map, largest_contours, -1, new Scalar(0, 255, 0, 255), 3);


                }
            }

            }
        simpleBitmap = Bitmap.createBitmap(map.width(), map.height(), Bitmap.Config.RGB_565);
        firstBitmap = true;
        Utils.matToBitmap(dst, simpleBitmap);
        handler.sendEmptyMessage(1);

//        Utils.matToBitmap(dst, simpleBitmap);
//        imageView.setImageBitmap(simpleBitmap);
    }

    //OpenCV库静态加载并初始化
    private void staticLoadCVLibraries() {
        boolean load = OpenCVLoader.initDebug();
        if (load) {
            Log.e("CV", "Open CV Libraries loaded...");
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message.what == 1) {
            imageView.setImageBitmap(simpleBitmap);
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_reset:
                record = 0;
                break;
        }
    }
}
