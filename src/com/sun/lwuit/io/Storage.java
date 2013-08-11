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

package com.sun.lwuit.io;

import com.sun.lwuit.io.impl.IOImplementation;
import com.sun.lwuit.io.util.Util;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstracts the underlying application specific storage system such as RMS
 *
 * @author Shai Almog
 */
public class Storage {
    private CacheMap cache = new CacheMap();
    private static Storage INSTANCE;

    /**
     * Indicates the caching size, storage can be pretty slow
     * 
     * @param size size in elements (not kb!)
     */
    public void setHardCacheSize(int size) {
        cache.setCacheSize(size);
    }

    /**
     * This method must be invoked before using the storage otherwise some platforms
     * might fail without the application data.
     *
     * @param data either the name of the application e.g. on CDC platforms or
     * a context object on other platforms
     */
    public static void init(Object data) {
        IOImplementation.getInstance().setStorageData(data);
        INSTANCE = new Storage();
    }

    /**
     * Returns true if the storage is initialized
     * 
     * @return true if the storage is initialized
     */
    public static boolean isInitialized(){
        return INSTANCE != null;
    }
    
    /**
     * Returns the storage instance or null if the storage wasn't initialized using
     * a call to init(String) first.
     *
     * @return storage instance
     */
    public static Storage getInstance() {
        return INSTANCE;
    }

    
    /**
     * Flush the storage cache allowing implementations that cache storage objects
     * to store
     */
    public void flushStorageCache() {
        IOImplementation.getInstance().flushStorageCache();
    }

    /**
     * Deletes the given file name from the storage
     *
     * @param name the name of the storage file
     */
    public void deleteStorageFile(String name) {
        IOImplementation.getInstance().deleteStorageFile(name);
        cache.delete(name);
    }

    /**
     * Deletes all the files in the application storage
     */
    public void clearStorage() {
        IOImplementation.getInstance().clearStorage();
        cache.clearAllCache();
    }

    /**
     * Creates an output stream to the storage with the given name
     *
     * @param name the storage file name
     * @return an output stream of limited capcity
     */
    public OutputStream createOutputStream(String name) throws IOException {
        return IOImplementation.getInstance().createStorageOutputStream(name);
    }

    /**
     * Creates an input stream to the given storage source file
     *
     * @param name the name of the source file
     * @return the input stream
     */
    public InputStream createInputStream(String name) throws IOException {
        return IOImplementation.getInstance().createStorageInputStream(name);
    }

    /**
     * Returns true if the given storage file exists
     *
     * @param name the storage file name
     * @return true if it exists
     */
    public boolean exists(String name) {
        return IOImplementation.getInstance().storageFileExists(name);
    }

    /**
     * Lists the names of the storage files
     *
     * @return the names of all the storage files
     */
    public String[] listEntries() {
        return IOImplementation.getInstance().listStorageEntries();
    }

    /**
     * Writes the given object to storage assuming it is an externalizable type
     * or one of the supported types
     *
     * @param name store name
     * @param o object to store
     * @return true for success, false for failue
     */
    public boolean writeObject(String name, Object o) {
        cache.put(name, o);
        DataOutputStream d = null;
        try {
            d = new DataOutputStream(createOutputStream(name));
            Util.writeObject(o, d);
            d.close();
            return true;
        } catch(Exception err) {
            err.printStackTrace();
            IOImplementation.getInstance().deleteStorageFile(name);
            IOImplementation.getInstance().cleanup(d);
            return false;
        }
    }

    /**
     * Reads the object from the storage, returns null if the object isn't there
     *
     * @param name name of the store
     * @return object stored under that name
     */
    public Object readObject(String name) {
        Object o = cache.get(name);
        if(o != null) {
            return o;
        }
        DataInputStream d = null;
        try {
            if(!exists(name)) {
                return null;
            }
            d = new DataInputStream(createInputStream(name));
            o = Util.readObject(d);
            cache.put(name, o);
            return o;
        } catch(Exception err) {
            IOImplementation.getInstance().cleanup(d);
            return null;
        }
    }
}
