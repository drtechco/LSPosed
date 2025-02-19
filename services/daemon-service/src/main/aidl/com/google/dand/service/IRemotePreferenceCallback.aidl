package com.google.dand.service;

interface IRemotePreferenceCallback {
    oneway void onUpdate(in Bundle map);
}
