/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Fast unchecked version of AbstractBytes
 */
public class UncheckedBytes<Underlying> extends AbstractBytes<Underlying> {
    public UncheckedBytes(@NotNull Bytes underlyingBytes) throws IllegalStateException {
        super(underlyingBytes.bytesStore(), underlyingBytes.writePosition(), underlyingBytes.writeLimit());
        readPosition(underlyingBytes.readPosition());
    }

    @NotNull
    public Bytes<Underlying> unchecked(boolean unchecked) {
        return this;
    }

    @Override
    void writeCheckOffset(long offset, long adding) {
    }

    @Override
    void readCheckOffset(long offset, long adding, boolean given) {
    }

    @Override
    void prewriteCheckOffset(long offset, long subtracting) {
    }

    @NotNull
    @Override
    public Bytes<Underlying> readPosition(long position) {
        readPosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readLimit(long limit) {
        writePosition = limit;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writePosition(long position) {
        writePosition = position;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> readSkip(long bytesToSkip) {
        readPosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeSkip(long bytesToSkip) {
        writePosition += bytesToSkip;
        return this;
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeLimit(long limit) {
        writeLimit = limit;
        return this;
    }

    @NotNull
    @Override
    public BytesStore<Bytes<Underlying>, Underlying> copy() {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    @Override
    protected long readOffsetPositionMoved(long adding) {
        long offset = readPosition;
        readPosition += adding;
        return offset;
    }

    @Override
    protected long writeOffsetPositionMoved(long adding) {
        long oldPosition = writePosition;
        writePosition += adding;
        return oldPosition;
    }

    @Override
    protected long prewriteOffsetPositionMoved(long subtracting) throws BufferOverflowException, IORuntimeException {
        return readPosition -= subtracting;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull BytesStore bytes, long offset, long length)
            throws IORuntimeException, BufferOverflowException, IllegalArgumentException {
        if (length == 8) {
            writeLong(bytes.readLong(offset));

        } else if (bytes.underlyingObject() == null && length >= 32) {
            rawCopy(bytes, offset, length);

        } else {
            super.write(bytes, offset, length);
        }
        return this;
    }

    @NotNull
    public Bytes<Underlying> append8bit(@NotNull CharSequence cs)
            throws BufferOverflowException, BufferUnderflowException, IORuntimeException {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }

        int length = cs.length();
        long offset = writeOffsetPositionMoved(length);
        for (int i = 0; i < length; i++) {
            char c = cs.charAt(i);
            if (c > 255) c = '?';
            writeByte(offset, (byte) c);
        }
        return this;
    }

    public void rawCopy(@NotNull BytesStore bytes, long offset, long length)
            throws IORuntimeException, BufferOverflowException, IllegalArgumentException {
        long len = Math.min(writeRemaining(), Math.min(bytes.readRemaining(), length));
        if (len > 0) {
            writeCheckOffset(writePosition(), len);
            OS.memory().copyMemory(bytes.address(offset), address(writePosition()), len);
            writeSkip(len);
        }
    }
}
