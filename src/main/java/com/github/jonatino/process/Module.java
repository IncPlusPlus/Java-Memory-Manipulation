/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jonathan Beaudoin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.jonatino.process;


import com.github.jonatino.misc.Cacheable;
import com.github.jonatino.misc.MemoryBuffer;
import com.sun.jna.Pointer;

public final class Module implements DataSource {

    private final Process process;
    private final String name;
    private final long address;
    private final int size;
    private final Pointer pointer;
    private MemoryBuffer data;

    public Module(Process process, String name, Pointer pointer, long size) {
        this.process = process;
        this.name = name;
        this.address = Pointer.nativeValue(pointer);
        this.size = (int) size;
        this.pointer = pointer;
    }

    public Process process() {
        return process;
    }

    public Pointer pointer() {
        return pointer;
    }

    public String name() {
        return name;
    }

    public int size() {
        return size;
    }

    public long address() {
        return address;
    }

    public MemoryBuffer data() {
        return data(false);
    }

    public MemoryBuffer data(boolean forceNew) {
        return data == null || forceNew ? data = process().read(pointer(), size()) : data;
    }

    @Override
    public MemoryBuffer read(Pointer offset, int size) {
        return process().read(Cacheable.pointer(address() + Pointer.nativeValue(offset)), size);
    }

    @Override
    public Process write(Pointer offset, MemoryBuffer buffer) {
        return process().write(Cacheable.pointer(address() + Pointer.nativeValue(offset)), buffer);
    }

    @Override
    public boolean canRead(Pointer offset, int size) {
        return process().canRead(Cacheable.pointer(address() + Pointer.nativeValue(offset)), size);
    }

    @Override
    public String toString() {
        return "Module{" + "name='" + name + '\'' + ", address=" + address + ", size=" + size + '}';
    }

}