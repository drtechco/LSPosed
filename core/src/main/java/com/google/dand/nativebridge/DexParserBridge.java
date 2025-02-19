package com.google.dand.nativebridge;

import com.google.libdandroid.api.utils.DexParser;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import dalvik.annotation.optimization.FastNative;

public class DexParserBridge {
    @FastNative
    public static native Object openDex(ByteBuffer data, long[] args) throws IOException;

    @FastNative
    public static native void closeDex(long cookie);

    @FastNative
    public static native void visitClass(long cookie, Object visitor, Class<DexParser.FieldVisitor> fieldVisitorClass, Class<DexParser.MethodVisitor> methodVisitorClass, Method classVisitMethod, Method fieldVisitMethod, Method methodVisitMethod, Method methodBodyVisitMethod, Method stopMethod);
}
