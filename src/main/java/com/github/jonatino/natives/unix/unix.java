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

package com.github.jonatino.natives.unix;

import com.sun.jna.*;

import java.util.List;

/**
 * Created by jonathan on 06/01/16.
 */
public final class unix {

	static {
		Native.register(NativeLibrary.getInstance("c"));
	}

	public static native long process_vm_readv(int pid, iovec local, long liovcnt, iovec remote, long riovcnt, long flags) throws LastErrorException;

	public static native long process_vm_writev(int pid, iovec local, long liovcnt, iovec remote, long riovcnt, long flags) throws LastErrorException;

	public static long process_vm_writeva(int pid, iovec local, long liovcnt, iovec remote, long riovcnt, long flags) throws LastErrorException {
		// System.out.println("S: [" + pid + "] Writing: " + hex(Pointer.nativeValue(local.iov_base)) + " > " + hex(Pointer.nativeValue(remote.iov_base)) + ": len: " + local.iov_len + " > " + remote.iov_len);
		return process_vm_writev(pid, local, liovcnt, remote, riovcnt, flags);
	};

	public static void process_vm_writev(int pid, iovec[] local, long liovcnt, iovec[] remote, long riovcnt, long flags) {
		if (riovcnt > liovcnt)
			throw new IllegalStateException("process_vm_writev: undefined index: riovcnt > liovcnt = " + riovcnt + " > " + liovcnt);
		for (int i = 0; i < liovcnt; i++) {
			if (local[i].iov_len < 0 || remote[i].iov_len < 0)
				throw new IllegalStateException("process_vm_writev: Negative iovec length: local: " + local[i].iov_len + " remote: " + remote[i].iov_len);
			// System.out.println("A: Writing: " + hex(Pointer.nativeValue(local[i].iov_base)) + " > " + hex(Pointer.nativeValue(remote[i].iov_base)));
			process_vm_writev(pid, local[i], 1, remote[i], 1, flags);
		}
	}

	public static class iovec extends Structure {

		public Pointer iov_base;
		public int iov_len;

		@Override
		protected List<String> getFieldOrder() {
			return createFieldsOrder("iov_base", "iov_len");
		}

	}

	public static String hex(long n) {
		return String.format("0x%8s", Long.toHexString(n)).replace(' ', '0');
	}

}