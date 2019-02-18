package com.aurora.store.installer;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class InstallerQueue {

    private Context mContext;
    private File mZipWithApkFiles;
    private boolean mShouldExtractZip;
    private List<File> mApkFiles;
    private File mCacheDirectory;
    private long mId;

    InstallerQueue(Context c, List<File> apkFiles, long id) {
        mContext = c;
        mApkFiles = apkFiles;
        mId = id;
    }

    InstallerQueue(Context c, File zipWithApkFiles, long id) {
        mContext = c;
        mZipWithApkFiles = zipWithApkFiles;
        mShouldExtractZip = true;
        mId = id;
    }

    long getId() {
        return mId;
    }

    List<File> getApkFiles() throws Exception {
        if (mShouldExtractZip)
            extractZip();
        return mApkFiles;
    }

    void clear() {
        if (mCacheDirectory != null) {
            deleteFile(mCacheDirectory);
        }
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles())
                deleteFile(child);
        }
        file.delete();
    }

    private void extractZip() throws Exception {
        createCacheDir();

        ZipFile zipFile = new ZipFile(mZipWithApkFiles);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        mApkFiles = new ArrayList<>(zipFile.size());

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory() || !entry.getName().endsWith(".apk"))
                throw new IllegalArgumentException("No APKs in ZIP");

            File tempApkFile = new File(mCacheDirectory, entry.getName());

            FileOutputStream outputStream = new FileOutputStream(tempApkFile);
            InputStream inputStream = zipFile.getInputStream(entry);
            IOUtils.copy(inputStream, outputStream);

            outputStream.close();
            inputStream.close();

            mApkFiles.add(tempApkFile);
        }
        zipFile.close();
    }

    private void createCacheDir() {
        mCacheDirectory = new File(mContext.getCacheDir(), String.valueOf(System.currentTimeMillis()));
        mCacheDirectory.mkdirs();
    }
}
