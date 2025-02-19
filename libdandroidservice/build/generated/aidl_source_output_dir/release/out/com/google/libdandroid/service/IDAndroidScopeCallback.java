/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.google.libdandroid.service;
public interface IDAndroidScopeCallback extends android.os.IInterface
{
  /** Default implementation for IDAndroidScopeCallback. */
  public static class Default implements com.google.libdandroid.service.IDAndroidScopeCallback
  {
    @Override public void onScopeRequestPrompted(java.lang.String packageName) throws android.os.RemoteException
    {
    }
    @Override public void onScopeRequestApproved(java.lang.String packageName) throws android.os.RemoteException
    {
    }
    @Override public void onScopeRequestDenied(java.lang.String packageName) throws android.os.RemoteException
    {
    }
    @Override public void onScopeRequestTimeout(java.lang.String packageName) throws android.os.RemoteException
    {
    }
    @Override public void onScopeRequestFailed(java.lang.String packageName, java.lang.String message) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.google.libdandroid.service.IDAndroidScopeCallback
  {
    /** Construct the stub at attach it to the interface. */
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.google.libdandroid.service.IDAndroidScopeCallback interface,
     * generating a proxy if needed.
     */
    public static com.google.libdandroid.service.IDAndroidScopeCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.google.libdandroid.service.IDAndroidScopeCallback))) {
        return ((com.google.libdandroid.service.IDAndroidScopeCallback)iin);
      }
      return new com.google.libdandroid.service.IDAndroidScopeCallback.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      switch (code)
      {
        case INTERFACE_TRANSACTION:
        {
          reply.writeString(descriptor);
          return true;
        }
      }
      switch (code)
      {
        case TRANSACTION_onScopeRequestPrompted:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onScopeRequestPrompted(_arg0);
          break;
        }
        case TRANSACTION_onScopeRequestApproved:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onScopeRequestApproved(_arg0);
          break;
        }
        case TRANSACTION_onScopeRequestDenied:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onScopeRequestDenied(_arg0);
          break;
        }
        case TRANSACTION_onScopeRequestTimeout:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          this.onScopeRequestTimeout(_arg0);
          break;
        }
        case TRANSACTION_onScopeRequestFailed:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          this.onScopeRequestFailed(_arg0, _arg1);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.google.libdandroid.service.IDAndroidScopeCallback
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void onScopeRequestPrompted(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onScopeRequestPrompted, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onScopeRequestApproved(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onScopeRequestApproved, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onScopeRequestDenied(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onScopeRequestDenied, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onScopeRequestTimeout(java.lang.String packageName) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onScopeRequestTimeout, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
      @Override public void onScopeRequestFailed(java.lang.String packageName, java.lang.String message) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(packageName);
          _data.writeString(message);
          boolean _status = mRemote.transact(Stub.TRANSACTION_onScopeRequestFailed, _data, null, android.os.IBinder.FLAG_ONEWAY);
        }
        finally {
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_onScopeRequestPrompted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
    static final int TRANSACTION_onScopeRequestApproved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
    static final int TRANSACTION_onScopeRequestDenied = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
    static final int TRANSACTION_onScopeRequestTimeout = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
    static final int TRANSACTION_onScopeRequestFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
  }
  public static final java.lang.String DESCRIPTOR = "com.google.libdandroid.service.IDAndroidScopeCallback";
  public void onScopeRequestPrompted(java.lang.String packageName) throws android.os.RemoteException;
  public void onScopeRequestApproved(java.lang.String packageName) throws android.os.RemoteException;
  public void onScopeRequestDenied(java.lang.String packageName) throws android.os.RemoteException;
  public void onScopeRequestTimeout(java.lang.String packageName) throws android.os.RemoteException;
  public void onScopeRequestFailed(java.lang.String packageName, java.lang.String message) throws android.os.RemoteException;
}
