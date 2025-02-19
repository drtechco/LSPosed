package com.google.dand.impl.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.dand.nativebridge.DexParserBridge;
import com.google.libdandroid.api.utils.DexParser;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DAndroidDexParser implements DexParser {
    long cookie;

    @NonNull
    final ByteBuffer data;
    @NonNull
    final StringId[] strings;
    @NonNull
    final TypeId[] typeIds;
    @NonNull
    final ProtoId[] protoIds;
    @NonNull
    final FieldId[] fieldIds;
    @NonNull
    final MethodId[] methodIds;
    @NonNull
    final Annotation[] annotations;
    @NonNull
    final Array[] arrays;

    public DAndroidDexParser(@NonNull ByteBuffer buffer, boolean includeAnnotations) throws IOException {
        if (!buffer.isDirect() || !buffer.asReadOnlyBuffer().hasArray()) {
            data = ByteBuffer.allocateDirect(buffer.capacity());
            data.put(buffer);
        } else {
            data = buffer;
        }
        try {
            long[] args = new long[2];
            args[1] = includeAnnotations ? 1 : 0;
            var out = (Object[]) DexParserBridge.openDex(buffer, args);
            cookie = args[0];
            // out[0]: String[]
            // out[1]: int[]
            // out[2]: int[][]
            // out[3]: int[]
            // out[4]: int[]
            // out[5]: int[]
            // out[6]: Object[]
            // out[7]: Object[]
            var strings = (Object[]) out[0];
            this.strings = new StringId[strings.length];
            for (int i = 0; i < strings.length; ++i) {
                this.strings[i] = new DAndroidStringId(i, strings[i]);
            }

            var typeIds = (int[]) out[1];
            this.typeIds = new TypeId[typeIds.length];
            for (int i = 0; i < typeIds.length; ++i) {
                this.typeIds[i] = new DAndroidTypeId(i, typeIds[i]);
            }

            var protoIds = (int[][]) out[2];
            this.protoIds = new ProtoId[protoIds.length];
            for (int i = 0; i < protoIds.length; ++i) {
                this.protoIds[i] = new DAndroidProtoId(i, protoIds[i]);
            }

            var fieldIds = (int[]) out[3];
            this.fieldIds = new FieldId[fieldIds.length / 3];
            for (int i = 0; i < this.fieldIds.length; ++i) {
                this.fieldIds[i] = new DAndroidFieldId(i, fieldIds[3 * i], fieldIds[3 * i + 1], fieldIds[3 * i + 2]);
            }

            var methodIds = (int[]) out[4];
            this.methodIds = new MethodId[methodIds.length / 3];
            for (int i = 0; i < this.methodIds.length / 3; ++i) {
                this.methodIds[i] = new DAndroidMethodId(i, methodIds[3 * i], methodIds[3 * i + 1], methodIds[3 * i + 2]);
            }

            if (out[5] != null && out[6] != null) {
                var a = (int[]) out[5];
                var b = (Object[]) out[6];
                this.annotations = new Annotation[a.length / 2];
                for (int i = 0; i < this.annotations.length; ++i) {
                    this.annotations[i] = new DAndroidAnnotation(a[2 * i], a[2 * i + 1], (int[]) b[2 * i], (Object[]) b[2 * i + 1]);
                }
            } else {
                this.annotations = new Annotation[0];
            }
            if (out[7] != null) {
                var b = (Object[]) out[7];
                this.arrays = new Array[b.length / 2];
                for (int i = 0; i < this.arrays.length; ++i) {
                    this.arrays[i] = new DAndroidArray((int[]) b[2 * i], (Object[]) b[2 * i + 1]);
                }
            } else {
                this.arrays = new Array[0];
            }
        } catch (Throwable e) {
            throw new IOException("Invalid dex file", e);
        }
    }

    @Override
    synchronized public void close() {
        if (cookie != 0) {
            DexParserBridge.closeDex(cookie);
            cookie = 0;
        }
    }

    static class DAndroidId<Self extends Id<Self>> implements Id<Self> {
        final int id;

        DAndroidId(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public int compareTo(Self o) {
            return id - o.getId();
        }
    }

    static class DAndroidStringId extends DAndroidId<StringId> implements StringId {
        @NonNull
        final String string;

        DAndroidStringId(int id, @NonNull Object string) {
            super(id);
            this.string = (String) string;
        }

        @NonNull
        @Override
        public String getString() {
            return string;
        }
    }

    class DAndroidTypeId extends DAndroidId<TypeId> implements TypeId {
        @NonNull
        final StringId descriptor;

        DAndroidTypeId(int id, int descriptor) {
            super(id);
            this.descriptor = strings[descriptor];
        }

        @NonNull
        @Override
        public StringId getDescriptor() {
            return descriptor;
        }
    }

    class DAndroidProtoId extends DAndroidId<ProtoId> implements ProtoId {
        @NonNull
        final StringId shorty;
        @NonNull
        final TypeId returnType;
        @Nullable
        final TypeId[] parameters;

        DAndroidProtoId(int id, @NonNull int[] protoId) {
            super(id);
            this.shorty = strings[protoId[0]];
            this.returnType = typeIds[protoId[1]];
            if (protoId.length > 2) {
                this.parameters = new TypeId[protoId.length - 2];
                for (int i = 2; i < parameters.length; ++i) {
                    this.parameters[i] = typeIds[protoId[i]];
                }
            } else {
                this.parameters = null;
            }
        }

        @NonNull
        @Override
        public StringId getShorty() {
            return shorty;
        }

        @NonNull
        @Override
        public TypeId getReturnType() {
            return returnType;
        }

        @Nullable
        @Override
        public TypeId[] getParameters() {
            return parameters;
        }
    }

    class DAndroidFieldId extends DAndroidId<FieldId> implements FieldId {
        @NonNull
        final TypeId type;
        @NonNull
        final TypeId declaringClass;
        @NonNull
        final StringId name;

        DAndroidFieldId(int id, int type, int declaringClass, int name) {
            super(id);
            this.type = typeIds[type];
            this.declaringClass = typeIds[declaringClass];
            this.name = strings[name];
        }

        @NonNull
        @Override
        public TypeId getType() {
            return type;
        }

        @NonNull
        @Override
        public TypeId getDeclaringClass() {
            return declaringClass;
        }

        @NonNull
        @Override
        public StringId getName() {
            return name;
        }
    }

    class DAndroidMethodId extends DAndroidId<MethodId> implements MethodId {
        @NonNull
        final TypeId declaringClass;
        @NonNull
        final ProtoId prototype;
        @NonNull
        final StringId name;

        DAndroidMethodId(int id, int declaringClass, int prototype, int name) {
            super(id);
            this.declaringClass = typeIds[declaringClass];
            this.prototype = protoIds[prototype];
            this.name = strings[name];
        }

        @NonNull
        @Override
        public TypeId getDeclaringClass() {
            return declaringClass;
        }

        @NonNull
        @Override
        public ProtoId getPrototype() {
            return prototype;
        }

        @NonNull
        @Override
        public StringId getName() {
            return name;
        }
    }

    static class DAndroidArray implements Array {
        @NonNull
        final Value[] values;

        DAndroidArray(int[] elements, @NonNull Object[] values) {
            this.values = new Value[values.length];
            for (int i = 0; i < values.length; ++i) {
                this.values[i] = new DAndroidValue(elements[i], (ByteBuffer) values[i]);
            }
        }

        @NonNull
        @Override
        public Value[] getValues() {
            return values;
        }
    }

    class DAndroidAnnotation implements Annotation {
        int visibility;
        @NonNull
        final TypeId type;
        @NonNull
        final Element[] elements;

        DAndroidAnnotation(int visibility, int type, @NonNull int[] elements, @NonNull Object[] elementValues) {
            this.visibility = visibility;
            this.type = typeIds[type];
            this.elements = new Element[elementValues.length];
            for (int i = 0; i < elementValues.length; ++i) {
                this.elements[i] = new DAndroidElement(elements[i * 2], elements[i * 2 + 1], (ByteBuffer) elementValues[i]);
            }
        }

        @Override
        public int getVisibility() {
            return visibility;
        }

        @NonNull
        @Override
        public TypeId getType() {
            return type;
        }

        @NonNull
        @Override
        public Element[] getElements() {
            return elements;
        }
    }

    static class DAndroidValue implements Value {
        final int valueType;
        @Nullable
        final byte[] value;

        DAndroidValue(int valueType, @Nullable ByteBuffer value) {
            this.valueType = valueType;
            if (value != null) {
                this.value = new byte[value.remaining()];
                value.get(this.value);
            } else {
                this.value = null;
            }
        }

        @Nullable
        @Override
        public byte[] getValue() {
            return value;
        }

        @Override
        public int getValueType() {
            return valueType;
        }
    }

    class DAndroidElement extends DAndroidValue implements Element {
        @NonNull
        final StringId name;

        DAndroidElement(int name, int valueType, @Nullable ByteBuffer value) {
            super(valueType, value);
            this.name = strings[name];
        }

        @NonNull
        @Override
        public StringId getName() {
            return name;
        }
    }

    @NonNull
    @Override
    public StringId[] getStringId() {
        return strings;
    }

    @NonNull
    @Override
    public TypeId[] getTypeId() {
        return typeIds;
    }

    @NonNull
    @Override
    public FieldId[] getFieldId() {
        return fieldIds;
    }

    @NonNull
    @Override
    public MethodId[] getMethodId() {
        return methodIds;
    }

    @NonNull
    @Override
    public ProtoId[] getProtoId() {
        return protoIds;
    }

    @NonNull
    @Override
    public Annotation[] getAnnotations() {
        return annotations;
    }

    @NonNull
    @Override
    public Array[] getArrays() {
        return arrays;
    }

    @Override
    synchronized public void visitDefinedClasses(@NonNull ClassVisitor visitor) {
        if (cookie == 0) {
            throw new IllegalStateException("Closed");
        }
        var classVisitMethod = ClassVisitor.class.getDeclaredMethods()[0];
        var fieldVisitMethod = FieldVisitor.class.getDeclaredMethods()[0];
        var methodVisitMethod = MethodVisitor.class.getDeclaredMethods()[0];
        var methodBodyVisitMethod = MethodBodyVisitor.class.getDeclaredMethods()[0];
        var stopMethod = EarlyStopVisitor.class.getDeclaredMethods()[0];

        DexParserBridge.visitClass(cookie, visitor, FieldVisitor.class, MethodVisitor.class, classVisitMethod, fieldVisitMethod, methodVisitMethod, methodBodyVisitMethod, stopMethod);
    }
}
