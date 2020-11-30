/*
 *    Copyright 2016 Jonathan Beaudoin
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.jonatino.process.impl;

import com.github.jonatino.misc.Cacheable;
import com.github.jonatino.misc.MemoryBuffer;
import com.github.jonatino.misc.Strings;
import com.github.jonatino.natives.unix.CLink;
import com.github.jonatino.process.AbstractProcess;
import com.github.jonatino.process.Module;
import com.github.jonatino.process.Process;
import com.sun.jna.Pointer;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public final class UnsafeProcess extends AbstractProcess {

	private static final Unsafe unsafe;
	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = (Unsafe) field.get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public UnsafeProcess(int id) {
		super(id);
	}

	@Override
	public void initModules() {
		final Process _this = this;
		CLink.dl_iterator callback = new CLink.dl_iterator() {
			public int invoke(Pointer info, long size, long data) {
				CLink.dl_phdr_info phdr = new CLink.dl_phdr_info(info);
				phdr.read();
				long addr = phdr.dlpi_addr + phdr.dlpi_phdr.p_vaddr; // dlpi_phdr[0]
				long sz = phdr.dlpi_phdr.p_memsz;
				modules.put(phdr.dlpi_name, new Module(_this, phdr.dlpi_name, Pointer.createConstant(addr), sz, "rwxs"));

				return 0;
			}
		};
		CLink.INSTANCE.dl_iterate_phdr(callback, 0);
	}

	@Override
	public MemoryBuffer read(long address, int size) {
		MemoryBuffer buffer = Cacheable.buffer(size);
		unsafe.copyMemory(address, Pointer.nativeValue(buffer), size);
		return buffer;
	}

	@Override
	public void read(long address, int size, long target) {
		unsafe.copyMemory(address, target, size);
	}

	@Override
	public Process write(Pointer address, MemoryBuffer buffer) throws com.sun.jna.LastErrorException {
		unsafe.copyMemory(Pointer.nativeValue(address), Pointer.nativeValue(buffer), buffer.size());
		return this;
	}

	@Override
	public boolean readBoolean(long address) {
		return unsafe.getByte(address) > 1;
	}

	@Override
	public int readByte(long address) {
		return unsafe.getByte(address);
	}

	@Override
	public int readShort(long address) {
		return unsafe.getShort(address);
	}

	@Override
	public int readInt(long address) {
		return unsafe.getInt(address);
	}

	@Override
	public long readUnsignedInt(long address) {
		return Integer.toUnsignedLong(readInt(address));
	}

	@Override
	public long readLong(long address) {
		return unsafe.getLong(address);
	}

	@Override
	public float readFloat(long address) {
		return unsafe.getFloat(address);
	}

	@Override
	public double readDouble(long address) {
		return unsafe.getDouble(address);
	}

	@Override
	public String readString(long address, int length) {
		byte[] bytes = Cacheable.array(length);
		Cacheable.pointer(address).read(0, bytes, 0, length);
		return Strings.transform(bytes);
	}

	@Override
	public long readPointer(long address) {
		return unsafe.getLong(address);
	}

	@Override
	public Process writeBoolean(long address, boolean value) {
		unsafe.putByte(address, value ? (byte) 1 : (byte) 0);
		return this;
	}

	@Override
	public Process writeByte(long address, int value) {
		unsafe.putByte(address, (byte) value);
		return this;
	}

	@Override
	public Process writeShort(long address, int value) {
		unsafe.putShort(address, (short) value);
		return this;
	}

	@Override
	public Process writeInt(long address, int value) {
		unsafe.putInt(address, value);
		return this;
	}

	@Override
	public Process writeLong(long address, long value) {
		System.out.println("writing " + Long.toHexString(value) + " > " + Long.toHexString(address));
		unsafe.putLong(address, value);
		return this;
	}

	@Override
	public Process writeFloat(long address, float value) {
		unsafe.putFloat(address, value);
		return this;
	}

	@Override
	public Process writeDouble(long address, double value) {
		unsafe.putDouble(address, value);
		return this;
	}
	
	@Override
	public boolean canRead(Pointer address, int size) {
		try {
			read(address, size);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}