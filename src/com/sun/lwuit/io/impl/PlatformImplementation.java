/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.sun.lwuit.io.impl;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.sun.lwuit.io.FileSystemStorage;
import com.sun.lwuit.io.util.BufferedInputStream;
import com.sun.lwuit.io.util.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class PlatformImplementation extends IOImplementation {

    private Context getContext() {
        Object s = super.getStorageData();
        if (s == null || !(s instanceof Context)) {
            throw new RuntimeException("call com.sun.lwuit.io.Storage.init(myActivity) before using the storage mechanism!");
        }
        return (Context) s;
    }

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        if (url.startsWith("socket://")) {
            String host = url.replace("socket://", "");
            int index = url.indexOf(':');
            if (index <= 0) {
                throw new IOException("expecting host:port for socket connection");
            }
            int port = Integer.parseInt(url.substring(index + 1));
            host = host.substring(0, index);
            return new Socket(host, port);
        } else {
            URLConnection c = new URL(url).openConnection();
            c.setDoInput(read);
            c.setDoOutput(write);
            return c;
        }
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        ((HttpURLConnection) connection).setRequestProperty(key, val);
    }

    @Override
    public int getContentLength(Object connection) {
        return ((HttpURLConnection) connection).getContentLength();
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        return new BufferedOutputStream(((HttpURLConnection) connection).getOutputStream());
    }
    
    public boolean shouldWriteUTFAsGetBytes() {
        // if false, the superclass would use an OutputStreamWriter for
        // writing the body, which causes problems. maybe because it is not
        // flushed. 
        return true;
    }

    @Override
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        RandomAccessFile rf = new RandomAccessFile((String)connection, "rw");
        rf.seek(offset);
        FileOutputStream fc = new FileOutputStream(rf.getFD());
        BufferedOutputStream o = new BufferedOutputStream(fc, (String)connection);
        o.setConnection(rf);
        return o;
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        return new BufferedInputStream(((HttpURLConnection) connection).getInputStream());
    }

    @Override
    public OutputStream openFileOutputStream(String file) throws IOException {
        FileOutputStream fo = new FileOutputStream(file);
        BufferedOutputStream b = new BufferedOutputStream(fo);
        b.setConnection(fo);
        return b;
    }

    @Override
    public void cleanup(Object o) {
        if (o instanceof FileOutputStream) {
            try {
                // not using the IOImplementation.closingOutput(OutputStream s) method
                // as sync() should happen after flushing, no?
                //
                // For some reasons the Android guys chose not doing this by default:
                // http://android-developers.blogspot.com/2010/12/saving-data-safely.html
                // this seems to be a mistake of sacrificing stability for minor performance
                // gains which will only be noticeable on a server.
                ((FileOutputStream) o).getFD().sync();
            } catch (Throwable t) {
                Log.e("LWUIT", "problem syncing stream.", t);
            }
            super.cleanup(o);
        } else if (o instanceof RandomAccessFile) {
            try {
                super.cleanup(o);
                if (o != null) {
                    if (o instanceof RandomAccessFile) {
                        ((RandomAccessFile) o).close();
                    }
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } else {
            super.cleanup(o);
        }
    }

    @Override
    public InputStream openFileInputStream(String file) throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
        try {
            ((HttpURLConnection) connection).setRequestMethod(p ? "POST" : "GET");
        } catch (Exception e) {
            Log.e("LWUIT", "failed to setPostMethod", e);
        }
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        return ((HttpURLConnection) connection).getResponseCode();
    }

    @Override
    public String getResponseMessage(Object connection) throws IOException {
        return ((HttpURLConnection) connection).getResponseMessage();
    }

    @Override
    public String getHeaderField(String name, Object connection) throws IOException {
        return ((HttpURLConnection) connection).getHeaderField(name);
    }

    @Override
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        Map<String, List<String>> map = ((HttpURLConnection) connection).getHeaderFields();
        return map != null && map.containsKey(name) ? map.get(name).toArray(new String[0]) : new String[0];
    }

    @Override
    public void deleteStorageFile(String name) {
        this.getContext().deleteFile(name);
    }

    @Override
    public OutputStream createStorageOutputStream(String name) throws IOException {
        return this.getContext().openFileOutput(name, Context.MODE_PRIVATE);
    }

    @Override
    public InputStream createStorageInputStream(String name) throws IOException {
        return this.getContext().openFileInput(name);
    }

    @Override
    public boolean storageFileExists(String name) {
        String[] files = listStorageEntries();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String[] listStorageEntries() {
        return this.getContext().fileList();
    }

    @Override
    public String[] listFilesystemRoots() {
        File[] files = File.listRoots();
        if (files != null && files.length > 0) {
            String[] roots = new String[files.length];
            for (int i = 0; i < roots.length; i++) {
                roots[i] = files[i].getName();
            }
            return roots;
        }
        return new String[0];
    }

    @Override
    public String[] listFiles(String directory) throws IOException {
        File[] files = new File(directory).listFiles();
        if (files != null && files.length > 0) {
            String[] fileNames = new String[files.length];
            for (int i = 0; i < fileNames.length; i++) {
                fileNames[i] = files[i].getName();
            }
            return fileNames;
        }
        return new String[0];
    }

    @Override
    public long getRootSizeBytes(String root) {
        String[] roots = this.listFilesystemRoots();
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].equals(root)) {
                File r = new File(root);
                return r.getTotalSpace() - r.getUsableSpace();
            }
        }
        return 0;
    }

    @Override
    public long getRootAvailableSpace(String root) {
        String[] roots = this.listFilesystemRoots();
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].equals(root)) {
                File r = new File(root);
                return r.getUsableSpace();
            }
        }
        return 0;
    }

    @Override
    public void mkdir(String directory) {
        new File(directory).mkdir();
    }

    @Override
    public void deleteFile(String file) {
        new File(file).delete();
    }

    @Override
    public boolean isHidden(String file) {
        return new File(file).isHidden();
    }

    @Override
    public void setHidden(String file, boolean h) {
    }

    @Override
    public long getFileLength(String file) {
        return new File(file).length();
    }

    @Override
    public boolean isDirectory(String file) {
        return new File(file).isDirectory();
    }

    @Override
    public boolean exists(String file) {
        return new File(file).exists();
    }

    @Override
    public void rename(String file, String newName) {
        File f = new File(file);
        f.renameTo(new File(f.getParentFile(), newName));
    }

    @Override
    public char getFileSystemSeparator() {
        return File.separatorChar;
    }

    @Override
    public int getRootType(String root) {
        root = root.toLowerCase();
        File sdCard = Environment.getExternalStorageDirectory();
        if (sdCard != null && sdCard.equals(new File(root))) {
            return FileSystemStorage.ROOT_TYPE_SDCARD;
        }
        return FileSystemStorage.ROOT_TYPE_MAINSTORAGE;
    }
    
    @Override
    public void startThread(String name, Runnable r) {
        new Thread(Thread.currentThread().getThreadGroup(), r, name, 64 * 1024).start();
    }

    @Override
    public void printStackTraceToStream(Throwable t, Writer o) {
        super.printStackTraceToStream(t, o);
        PrintWriter p = new PrintWriter(o);
        t.printStackTrace(p);
    }
}
