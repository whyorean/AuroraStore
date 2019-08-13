package com.aurora.store.task;

import android.content.Context;
import android.content.ContextWrapper;

import com.aurora.store.utility.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

public class GZipTask extends ContextWrapper {

    public GZipTask(Context base) {
        super(base);
    }

    public boolean extract(File file) {
        try {
            String oldPath = file.getPath();
            String newPath = oldPath.replace("gzip", "obb");
            InputStream inputStream = new GZIPInputStream(new FileInputStream(file), 131072 /*Block Size*/);
            OutputStream outputStream = new FileOutputStream(newPath);
            IOUtils.copyLarge(inputStream, outputStream);
            file.delete();
            inputStream.close();
            outputStream.close();
            return true;
        } catch (IOException | IllegalStateException e) {
            Log.e(e.getMessage());
            return false;
        }
    }
}
