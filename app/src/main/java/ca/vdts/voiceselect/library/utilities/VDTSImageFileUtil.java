package ca.vdts.voiceselect.library.utilities;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.exifinterface.media.ExifInterface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class VDTSImageFileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSImageFileUtil.class);
    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH-mm-ss"
    );
    private static final DateTimeFormatter preciseDateTimeFormat = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH-mm-ss.SSS"
    );

    public VDTSImageFileUtil() {
    }

    public static String generateFileName(String label, boolean precise) {
        return label.concat(
                precise ?
                        preciseDateTimeFormat.format(LocalDateTime.now()) :
                        dateTimeFormat.format(LocalDateTime.now())
        );
    }

    public static void addGPS(String src, Location location) {
        File photo = new File(src);
        if (photo.exists()) {
            try {
                ExifInterface exif = new ExifInterface(photo.getCanonicalPath());
                exif.setGpsInfo(location);
                exif.saveAttributes();
            } catch (IOException var4) {
                LOG.error("Error writing GPS metadata to {}", src, var4);
            }
        }

    }

    public static File drawToBitmap(String src, String... text) throws IOException {
        File file = new File(src);
        List<String> lines = Arrays.asList(text);
        Bitmap bitmap = BitmapFactory.decodeFile(src);
        FileOutputStream out = new FileOutputStream(src);
        Bitmap.Config bitmapConfig = bitmap.getConfig();
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888;
        }

        bitmap = bitmap.copy(bitmapConfig, true);

        try {
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(1);
            paint.setColor(Color.rgb(0, 0, 255));
            paint.setStrokeWidth(12.0F);
            paint.setTextSize(30.0F);
            paint.setShadowLayer(2.0F, 0.0F, 1.0F, -16777216);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
            int line = 40;

            for(int i = 0; i < lines.size(); ++i) {
                if (!((String)lines.get(i)).isEmpty()) {
                    canvas.drawText((String)lines.get(i), 10.0F, (float)(line += 40), paint);
                }
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception var11) {
            var11.printStackTrace();
        }

        return file;
    }

    public static String encodeBase64(File imageFile) {
        Bitmap bm = BitmapFactory.decodeFile(imageFile.getPath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.getEncoder().encodeToString(b);
    }

    public static String getPath(final Context context, final Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String docId;
            String[] split;
            String type;
            if (isExternalStorageDocument(uri)) {
                docId = DocumentsContract.getDocumentId(uri);
                split = docId.split(":");
                type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else {
                if (isDownloadsDocument(uri)) {
                    docId = DocumentsContract.getDocumentId(uri);
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                    return getDataColumn(context, contentUri, (String)null, (String[])null);
                }

                if (isMediaDocument(uri)) {
                    docId = DocumentsContract.getDocumentId(uri);
                    split = docId.split(":");
                    type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    String selection = "_id=?";
                    String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, "_id=?", selectionArgs);
                }
            }
        } else {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, (String)null, (String[])null);
            }

            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        }

        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = new String[]{"_data"};

        String var8;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, (String)null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }

            int column_index = cursor.getColumnIndexOrThrow("_data");
            var8 = cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }

        }

        return var8;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
