package com.google.dand.service;

import com.google.dand.service.IRemotePreferenceCallback;

interface ILSPInjectedModuleService {
    int getFrameworkPrivilege();

    Bundle requestRemotePreferences(String group, IRemotePreferenceCallback callback);

    ParcelFileDescriptor openRemoteFile(String path);

    String[] getRemoteFileList();
}
