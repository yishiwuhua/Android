package cn.tsofts.android.core.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PictureUtil {
    /**
     * 读取照片旋转角度
     *
     * @param path 照片路径
     * @return 角度
     */
    public static int getPictureRotate(String path) {
        int rotate = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rotate;
    }

    /**
     * 旋转图片
     *
     * @param rotate 旋转角度
     * @param bitmap 图片
     * @return 旋转后的图片
     */
    public static Bitmap rotatePicture(int rotate, Bitmap bitmap) {
        Bitmap newBitmap;
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
        newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (newBitmap == null) {
            newBitmap = bitmap;
        }
        if (bitmap != newBitmap) {
            bitmap.recycle();//删除源文件？
        }
        return newBitmap;
    }

    /**
     * 压缩图片
     *
     * @param path         图片路径
     * @param inSampleSize 压缩比率
     * @return 压缩后的图片
     */
    public static Bitmap getCompressPicure(String path, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;  // 图片的大小设置为原来的十分之一
        return BitmapFactory.decodeFile(path, options);
    }

    /**
     * 保存Bitmap为图片
     *
     * @param bitmap   Bitmap
     * @param saveFile 图片路径及名称
     */
    public static void saveBitmap(Bitmap bitmap, File saveFile) {
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(saveFile);
            // 把数据写入文件，100表示不压缩
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                }
                if (bitmap != null) {
                    bitmap.recycle();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

