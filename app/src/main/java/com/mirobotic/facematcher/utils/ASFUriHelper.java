/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * CyberLink FaceMe (R) SDK
 * Copyright (C) 2018 CyberLink Corp. All rights reserved.
 * https://www.cyberlink.com
 */
package com.mirobotic.facematcher.utils;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;

/**
 * @see <a href="https://gist.github.com/asifmujteba/d89ba9074bc941de1eaa#file-asfurihelper">ASFUriHelper</a>
 */
public class ASFUriHelper {
    public static File getDirectory(Context context, Intent data) {
        if (data == null) return null;

        Uri uri = data.getData();
        if (uri == null) return null;

        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
        String path = getPath(context, docUri);
        if (path == null) return null;

        return new File(path);
    }

    private static final String[] knownFileMgrPrefixSet = {
            "/document/primary:",   // Android Default File explorer.
            "/file/sdcard/",        // ASUS File Manager.
    };

    private static String determineHardcodedPrefix(String filePath) {
        for (String knownPrefix : knownFileMgrPrefixSet) {
            if (filePath.startsWith(knownPrefix)) {
                return knownPrefix;
            }
        }
        return null;
    }

    public static File getFile(Context context, Intent data) {
        if (data == null) return null;

        Uri uri = data.getData();
        if (uri == null) return null;

        try {
            Uri docUri = DocumentsContract.buildDocumentUri(uri.getAuthority(), DocumentsContract.getTreeDocumentId(uri));
            String path = getPath(context, docUri);
            if (path == null) return null;
            return new File(path);
        } catch (Exception e) {
            // Something like example below:
            //   > uri: content://com.coloros.filemanager/file_share/storage/emulated/0/FaceMe/faceme.key
            //   > path: file_share/storage/emulated/0/FaceMe/faceme.key
            String filePath = uri.getPath();
            if (filePath == null) return null;

            // Workaround to concat possible file path.
            // Remove authority, it often occupies one segment.
            String localStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
            String knownPrefix = determineHardcodedPrefix(filePath);
            if (knownPrefix != null) {
                filePath = filePath.substring(knownPrefix.length());
                return new File(localStorage, filePath);
            } else if (filePath.contains(localStorage)) {
                if (!filePath.startsWith(localStorage)) {
                    filePath = filePath.substring(filePath.indexOf(localStorage));
                }

                return new File(filePath);
            }
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + File.separator + split[1];
                }

                // TODO handle non-primary volumes
                return "/storage/" + split[0] + "/" + split[1];
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("raw".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + File.separator + split[1];
                }
                if ("downloads".equalsIgnoreCase(type) && split.length == 1) {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                }

                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
