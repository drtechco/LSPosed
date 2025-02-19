package com.google.libdandroid.service;

import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class DAndroidServiceHelper {

    /**
     * Callback interface for DAndroid service.
     */
    public interface OnServiceListener {
        /**
         * Callback when the service is connected.<br/>
         * This method could be called multiple times if multiple DAndroid frameworks exist.
         *
         * @param service Service instance
         */
        void onServiceBind(@NonNull DAndroidService service);

        /**
         * Callback when the service is dead.
         */
        void onServiceDied(@NonNull DAndroidService service);
    }

    private static final String TAG = "DAndroidServiceHelper";
    private static final Set<DAndroidService> mCache = new HashSet<>();
    private static OnServiceListener mListener = null;

    static void onBinderReceived(IBinder binder) {
        if (binder == null) return;
        synchronized (mCache) {
            try {
                var service = new DAndroidService(IDAndroidService.Stub.asInterface(binder));
                if (mListener == null) {
                    mCache.add(service);
                } else {
                    binder.linkToDeath(() -> mListener.onServiceDied(service), 0);
                    mListener.onServiceBind(service);
                }
            } catch (Throwable t) {
                Log.e(TAG, "onBinderReceived", t);
            }
        }
    }

    /**
     * Register a ServiceListener to receive service binders from DAndroid frameworks.<br/>
     * This method should only be called once.
     *
     * @param listener Listener to register
     */
    public static void registerListener(OnServiceListener listener) {
        synchronized (mCache) {
            mListener = listener;
            if (!mCache.isEmpty()) {
                for (var it = mCache.iterator(); it.hasNext(); ) {
                    try {
                        var service = it.next();
                        service.getRaw().asBinder().linkToDeath(() -> mListener.onServiceDied(service), 0);
                        mListener.onServiceBind(service);
                    } catch (Throwable t) {
                        Log.e(TAG, "registerListener", t);
                        it.remove();
                    }
                }
                mCache.clear();
            }
        }
    }
}
