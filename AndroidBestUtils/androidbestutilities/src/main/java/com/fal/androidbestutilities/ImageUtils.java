package com.fal.androidbestutilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by FVolodia on 16.01.17.
 */

public class ImageUtils {

    private static final float BITMAP_SCALE = 0.4f;
    private static final float BLUR_RADIUS = 16.0f;


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap blur(Context ctx, Bitmap image) {
        int width = Math.round(image.getWidth() * BITMAP_SCALE);
        int height = Math.round(image.getHeight() * BITMAP_SCALE);

        Bitmap inputBitmap = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);

        RenderScript rs = RenderScript.create(ctx);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        Allocation tmpIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);

        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);

        return outputBitmap;
    }

    public static Bitmap getScreenshot(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
//        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        v.draw(c);
        return b;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap getBlurScreenshot(View v) {
        Bitmap screenshot = getScreenshot(v);
        return blur(v.getContext(), screenshot);
    }

    public static Bitmap rotateBitmap(Bitmap source, int angle) {
        Log.d("LOG", "rotateBitmap from " + source.getWidth() + "*" + source.getHeight());
        Matrix matrix = new Matrix();
        matrix.postRotate(360 - angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap mirrorBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
    }

    /**
     * This is a method for load a scaled down version into memory
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * This is a method for load a scaled down version into memory
     */
    public static Bitmap decodeSampledBitmapResource(byte[] data, int reqWidth, int reqHeight, int rotationAngle) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        if (reqWidth == -1) reqWidth = options.outWidth;
        if (reqHeight == -1) reqHeight = options.outHeight;

        float ratio = (float) options.outWidth / (float) options.outHeight;
        if (reqHeight == 0 && reqWidth > 0) reqHeight = (int) (reqWidth / ratio);
        if (reqWidth == 0 && reqHeight > 0) reqWidth = (int) (reqHeight * ratio);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        Bitmap returnedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        Log.d("LOG", "source bitmap width - " + returnedBitmap.getWidth() + " height - " + returnedBitmap.getHeight());

        if (returnedBitmap.getWidth() > returnedBitmap.getHeight())
            returnedBitmap = rotateBitmap(returnedBitmap, rotationAngle);

        returnedBitmap = scaleCenterCrop(returnedBitmap, returnedBitmap.getWidth(), returnedBitmap.getWidth());

        Log.d("LOG", "returned Bitmap width - " + returnedBitmap.getWidth() + " and height - " + returnedBitmap.getHeight());

        try {
            return mirrorBitmap(returnedBitmap);
        } catch (OutOfMemoryError outOfMemoryError) {
            return null;
        }
    }


    /**
     * This is a method to calculate a sample size value that is a power of two based on a target width and height
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    || (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    public static Bitmap scaleCenterCrop(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now
        // be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }



    public static Bitmap trimToSquare(Bitmap src){
        int width = src.getWidth();
        int height = src.getHeight();
        int crop = (width - height) / 2;
        if (width >= height){
            src = Bitmap.createBitmap(src, crop, 0, height, height);
        } else {
            crop = (height - width) / 2;
            src = Bitmap.createBitmap(src, 0, crop, width, width);
        }
        return src;
    }

    public static File saveBitmapToFile(String name, Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + name + ".jpg");

        file.createNewFile();
        FileOutputStream fo = new FileOutputStream(file);
        fo.write(bytes.toByteArray());
        fo.close();

        return file;
    }

}
