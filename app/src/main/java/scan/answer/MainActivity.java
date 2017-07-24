package scan.answer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_MEAN_C;


public class MainActivity extends Activity {
    private ImageView imageView;
    private Bitmap simpleBitmap;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        staticLoadCVLibraries();

        imageView = (ImageView) findViewById(R.id.show_img);

        loadingImg();
    }

    private void loadingImg() {
        String url = "http://api.tederen.com/file/image/5068";
        Glide.with(this).load(url).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                if (resource != null) {
                    simpleBitmap = resource;
                    imageView.setImageBitmap(simpleBitmap);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    /**
     * 图片灰度化
     */
    private void convertGray() {
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(simpleBitmap, src);
        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(temp, dst, Imgproc.COLOR_BGR2GRAY);
        Utils.matToBitmap(dst, simpleBitmap);
        imageView.setImageBitmap(simpleBitmap);
    }

    /**
     * 图片二值化
     */
    private void blackAndWhite() {
        Mat src = new Mat();
        Mat gray = new Mat();
        Mat edge = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(simpleBitmap, src);

        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        Imgproc.blur(gray, edge, new Size(3, 3));

        Imgproc.adaptiveThreshold(edge, dst, 255, ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 0);

        Utils.matToBitmap(dst, simpleBitmap);
        imageView.setImageBitmap(simpleBitmap);

    }

    /**
     * 还原
     *
     * @param view
     */
    protected void onReset(View view) {
        loadingImg();
    }

    /**
     * 灰度化
     *
     * @param view
     */
    protected void onGray(View view) {
        convertGray();
    }

    protected void onBinaryzation(View view) {
        blackAndWhite();
    }
    protected void onScan(View view) {
        permission();
    }

    private void intent(){
        Intent intent = new Intent(MainActivity.this,ScanClipActivity.class);
        startActivity(intent);
    }
    //OpenCV库静态加载并初始化
    private void staticLoadCVLibraries() {
        boolean load = OpenCVLoader.initDebug();
        if (load) {
            Log.e("CV", "Open CV Libraries loaded...");
        }
    }


    public void permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            //sd卡权限
//            int SDcardPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //调用相机权限
            int CAMERA = checkSelfPermission(Manifest.permission.CAMERA);
            //存储未开启的权限的指令
            List<String> permissions = new ArrayList<String>();

//            if (SDcardPermission != PackageManager.PERMISSION_GRANTED) {
//                //把没有获取的权限加入到oermissions中
//                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (CAMERA != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);

            }
            //当permissions不为空时，表示有权限未开启
            if (!permissions.isEmpty()) {
                //申请开启一组权限
                requestPermissions(permissions.toArray(new String[permissions.size()]), 1);
            } else {
                intent();
            }
        }else {
            intent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                List<Integer> list = new ArrayList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {


                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        list.add(i);
                    }
                }
                if (list.size() != 0) {
                    Log.e("msg",list.size() +" ");
                    //权限未开启
                } else {
                    //用户同意开启权限
                    intent();
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }




}
