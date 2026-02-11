package pictures.tristagram.lina.bodytune;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WebAppInterface {
    private final Context mContext;

    WebAppInterface(Context context) {
        mContext = context;
    }

    @JavascriptInterface
    public void exportCSV(String data, String fileName) {
        try {
            // Создаем временный файл
            File cachePath = new File(mContext.getCacheDir(), "exports");
            cachePath.mkdirs();
            File newFile = new File(cachePath, fileName);
            FileOutputStream stream = new FileOutputStream(newFile);
            stream.write(data.getBytes(StandardCharsets.UTF_8));
            stream.close();

            // Открываем диалог "Поделиться"
            Uri contentUri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileprovider", newFile);
            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, "text/csv");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                mContext.startActivity(Intent.createChooser(shareIntent, "Сохранить отчет"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public String getLanguage() {
        return java.util.Locale.getDefault().getLanguage();
    }

    @JavascriptInterface
    public String getTranslations(String lang) {
        try {
            InputStream is = mContext.getAssets().open("locales/" + lang + ".json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "{}";
        }
    }
}