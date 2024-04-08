/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.testing.impl;

import static android.graphics.BitmapFactory.decodeByteArray;
import static android.graphics.ImageFormat.JPEG;
import static android.graphics.ImageFormat.JPEG_R;
import static android.graphics.ImageFormat.YUV_420_888;

import static androidx.camera.testing.impl.ImageProxyUtil.createYUV420ImagePlanes;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Gainmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.internal.CameraCaptureResultImageInfo;
import androidx.camera.testing.impl.fakes.FakeCameraCaptureResult;
import androidx.camera.testing.impl.fakes.FakeImageProxy;
import androidx.camera.testing.impl.fakes.FakeJpegPlaneProxy;

import java.io.ByteArrayOutputStream;

/**
 * Generates images for testing.
 *
 * <p> The images generated by this class contains 4 color blocks follows the pattern below. Each
 * block have the same size and covers 1/4 of the image.
 *
 * <pre>
 * ------------------
 * |   red  | green |
 * ------------------
 * |  blue | yellow |
 * ------------------
 * </pre>
 *
 * <p> The gain map generated by this class contains 4 gray scale blocks follows the pattern below.
 * Each block have the same size and covers 1/4 of the image.
 *
 * <pre>
 * ------------------
 * |      black     |
 * ------------------
 * |    dark gray   |
 * ------------------
 * |      gray      |
 * ------------------
 * |      white     |
 * ------------------
 * </pre>
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TestImageUtil {

    @VisibleForTesting
    public static final int COLOR_BLACK = 0xFF000000;
    @VisibleForTesting
    public static final int COLOR_DARK_GRAY = 0xFF404040;
    @VisibleForTesting
    public static final int COLOR_GRAY = 0xFF808080;
    @VisibleForTesting
    public static final int COLOR_WHITE = 0xFFFFFFFF;

    private TestImageUtil() {
    }


    /**
     * Creates a [FakeImageProxy] with YUV format.
     *
     * TODO(b/245940015): fix the content of the image to match the value of {@link #createBitmap}.
     */
    @NonNull
    public static FakeImageProxy createYuvFakeImageProxy(@NonNull ImageInfo imageInfo,
            int width, int height) {
        FakeImageProxy image = new FakeImageProxy(imageInfo);
        image.setFormat(YUV_420_888);
        image.setPlanes(createYUV420ImagePlanes(width, height, 1, 1, false, false));
        image.setWidth(width);
        image.setHeight(height);
        return image;
    }

    /**
     * Creates a {@link FakeImageProxy} from JPEG bytes.
     */
    @NonNull
    public static FakeImageProxy createJpegFakeImageProxy(@NonNull ImageInfo imageInfo,
            @NonNull byte[] jpegBytes) {
        Bitmap bitmap = decodeByteArray(jpegBytes, 0, jpegBytes.length);
        return createJpegFakeImageProxy(imageInfo, jpegBytes, bitmap.getWidth(),
                bitmap.getHeight());
    }

    /**
     * Creates a {@link FakeImageProxy} from JPEG bytes of JPEG/R.
     */
    @NonNull
    public static FakeImageProxy createJpegrFakeImageProxy(@NonNull ImageInfo imageInfo,
            @NonNull byte[] jpegBytes) {
        Bitmap bitmap = decodeByteArray(jpegBytes, 0, jpegBytes.length);
        return createJpegrFakeImageProxy(imageInfo, jpegBytes, bitmap.getWidth(),
                bitmap.getHeight());
    }

    /**
     * Creates a {@link FakeImageProxy} from JPEG bytes.
     */
    @NonNull
    public static FakeImageProxy createJpegFakeImageProxy(@NonNull ImageInfo imageInfo,
            @NonNull byte[] jpegBytes, int width, int height) {
        FakeImageProxy image = new FakeImageProxy(imageInfo);
        image.setFormat(JPEG);
        image.setPlanes(new FakeJpegPlaneProxy[]{new FakeJpegPlaneProxy(jpegBytes)});
        image.setWidth(width);
        image.setHeight(height);
        return image;
    }

    /**
     * Creates a {@link FakeImageProxy} from JPEG bytes of JPEG/R.
     */
    @NonNull
    public static FakeImageProxy createJpegrFakeImageProxy(@NonNull ImageInfo imageInfo,
            @NonNull byte[] jpegBytes, int width, int height) {
        FakeImageProxy image = new FakeImageProxy(imageInfo);
        image.setFormat(JPEG_R);
        image.setPlanes(new FakeJpegPlaneProxy[]{new FakeJpegPlaneProxy(jpegBytes)});
        image.setWidth(width);
        image.setHeight(height);
        return image;
    }

    /**
     * Creates a {@link FakeImageProxy} from JPEG bytes.
     */
    @NonNull
    public static FakeImageProxy createJpegFakeImageProxy(@NonNull byte[] jpegBytes) {
        return createJpegFakeImageProxy(
                new CameraCaptureResultImageInfo(new FakeCameraCaptureResult()), jpegBytes);
    }

    /**
     * Creates a {@link FakeImageProxy} from JPEG bytes of JPEG/R.
     */
    @NonNull
    public static FakeImageProxy createJpegrFakeImageProxy(@NonNull byte[] jpegBytes) {
        return createJpegrFakeImageProxy(
                new CameraCaptureResultImageInfo(new FakeCameraCaptureResult()), jpegBytes);
    }

    /**
     * Generates a JPEG image.
     */
    @NonNull
    public static byte[] createJpegBytes(int width, int height) {
        Bitmap bitmap = createBitmap(width, height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generates a JPEG/R image.
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    public static byte[] createJpegrBytes(int width, int height) {
        Bitmap bitmap = createBitmapWithGainmap(width, height);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generates a A24 problematic JPEG image.
     */
    @NonNull
    public static byte[] createA24ProblematicJpegByteArray(int width, int height) {
        byte[] incorrectHeaderByteData =
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe1, (byte) 0xff,
                        (byte) 0x7c, (byte) 0x45, (byte) 0x78, (byte) 0x69, (byte) 0x66,
                        (byte) 0x00, (byte) 0x00};
        byte[] jpegBytes = createJpegBytes(width, height);
        byte[] result = new byte[incorrectHeaderByteData.length + jpegBytes.length];
        System.arraycopy(incorrectHeaderByteData, 0, result, 0, incorrectHeaderByteData.length);
        System.arraycopy(jpegBytes, 0, result, incorrectHeaderByteData.length, jpegBytes.length);
        return result;
    }

    /**
     * Generates a {@link Bitmap} image and paints it with 4 color blocks.
     */
    @NonNull
    public static Bitmap createBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int centerX = width / 2;
        int centerY = height / 2;
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, centerX, centerY, createPaint(Color.RED));
        canvas.drawRect(centerX, 0, width, centerY, createPaint(Color.GREEN));
        canvas.drawRect(centerX, centerY, width, height, createPaint(Color.YELLOW));
        canvas.drawRect(0, centerY, centerX, height, createPaint(Color.BLUE));
        return bitmap;
    }

    /**
     * Generates a {@link Bitmap} image (contains 4 color blocks) with gain map (contains 4 gray
     * scale blocks) set.
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    public static Bitmap createBitmapWithGainmap(int width, int height) {
        Bitmap bitmap = createBitmap(width, height);
        Api34Impl.setGainmap(bitmap, createGainmap(width, height));
        return bitmap;
    }

    /**
     * Generates a {@link Gainmap} image and paints it with 4 gray scale blocks.
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull
    public static Gainmap createGainmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int oneFourthY = height / 4;
        int twoFourthsY = oneFourthY * 2;
        int threeFourthsY = oneFourthY * 3;
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, width, oneFourthY, createPaint(COLOR_BLACK));
        canvas.drawRect(0, oneFourthY, width, twoFourthsY, createPaint(COLOR_DARK_GRAY));
        canvas.drawRect(0, twoFourthsY, width, threeFourthsY, createPaint(COLOR_GRAY));
        canvas.drawRect(0, threeFourthsY, width, height, createPaint(COLOR_WHITE));
        return Api34Impl.createGainmap(bitmap);
    }

    /**
     * Rotates the bitmap clockwise by the given degrees.
     */
    @NonNull
    public static Bitmap rotateBitmap(@NonNull Bitmap bitmap, int rotationDegrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                true);
    }

    /**
     * Calculates the average color difference between the 2 JPEG images.
     */
    public static int getAverageDiff(@NonNull byte[] jpeg1, @NonNull byte[] jpeg2) {
        return getAverageDiff(
                decodeByteArray(jpeg1, 0, jpeg1.length),
                decodeByteArray(jpeg2, 0, jpeg2.length));
    }

    /**
     * Calculates the average color difference between the 2 bitmaps.
     */
    public static int getAverageDiff(@NonNull Bitmap bitmap1, @NonNull Bitmap bitmap2) {
        checkState(bitmap1.getWidth() == bitmap2.getWidth());
        checkState(bitmap1.getHeight() == bitmap2.getHeight());
        int totalDiff = 0;
        for (int i = 0; i < bitmap1.getWidth(); i++) {
            for (int j = 0; j < bitmap1.getHeight(); j++) {
                totalDiff += calculateColorDiff(bitmap1.getPixel(i, j), bitmap2.getPixel(i, j));
            }
        }
        return totalDiff / (bitmap1.getWidth() * bitmap2.getHeight());
    }

    /**
     * Calculates the average color difference, between the given image/crop rect and the color.
     *
     * <p>This method is used for checking the content of an image is correct.
     */
    public static int getAverageDiff(@NonNull Bitmap bitmap, @NonNull Rect rect, int color) {
        int totalDiff = 0;
        for (int i = rect.left; i < rect.right; i++) {
            for (int j = rect.top; j < rect.bottom; j++) {
                totalDiff += calculateColorDiff(bitmap.getPixel(i, j), color);
            }
        }
        return totalDiff / (rect.width() * rect.height());
    }

    /**
     * Calculates the difference between 2 colors.
     *
     * <p>The difference is calculated as the average difference of each R, G and B color
     * components.
     */
    private static int calculateColorDiff(int color1, int color2) {
        int diff = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            diff += Math.abs(((color1 >> shift) & 0xFF) - ((color2 >> shift) & 0xFF));
        }
        return diff / 3;
    }

    /**
     * Creates a FILL paint with the given color.
     */
    private static Paint createPaint(int color) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        return paint;
    }

    @RequiresApi(34)
    private static class Api34Impl {
        @DoNotInline
        static Gainmap createGainmap(@NonNull Bitmap bitmap) {
            return new Gainmap(bitmap);
        }

        @DoNotInline
        static void setGainmap(@NonNull Bitmap bitmap, @NonNull Gainmap gainmap) {
            bitmap.setGainmap(gainmap);
        }

        // This class is not instantiable.
        private Api34Impl() {
        }
    }
}
