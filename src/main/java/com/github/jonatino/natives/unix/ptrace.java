package com.github.jonatino.natives.unix;

import com.github.jonatino.misc.Cacheable;
import com.github.jonatino.misc.MemoryBuffer;
import com.sun.jna.*;

import java.util.List;

public final class ptrace {

	// javah -jni -classpath "../../jna-4.3.0.jar:../bin" com.github.jonatino.natives.unix.ptracec

	// http://syprog.blogspot.sk/2012/03/linux-threads-through-magnifier-local.html
	// As Linux follows AMD64 calling convention when running in 64 bits, function parameters and system call arguments are passed via the following registers:
	// Function call: arguments 1 - 6 via RDI, RSI, RDX, RCX, R8, R9; additional arguments are passed on stack.
	// System call: arguments 1 - 6 via RDI, RSI, RDX, R10, R8, R9; additional arguments are passed on stack.
	// The return value is passed back via rax.

	// http://nullprogram.com/blog/2015/05/15/

	public static long ptracef(long request, int pid, long addr, long data) {
		return unixc.ptrace(request, pid, addr, data);
	}

	public static void attach(int pid) throws IllegalStateException {
		if (ptracef(PTRACE_ATTACH, pid, 0, 0) == -1)
			throw new IllegalStateException("ptrace(PTRACE_ATTACH) failed");
	}

	public static void detach(int pid) throws IllegalStateException {
		if (ptracef(PTRACE_DETACH, pid, 0, 0) == -1)
			throw new IllegalStateException("ptrace(PTRACE_DETACH) failed");
	}

	public static void cont(int pid) throws IllegalStateException {
		if (ptracef(PTRACE_CONT, pid, 0, 0) == -1)
			throw new IllegalStateException("ptrace(PTRACE_CONT) failed");
	}

	// Same as cont(int), but stops at the next system call
	public static void syscall(int pid) {
		if (ptracef(PTRACE_SYSCALL, pid, 0, 0) == -1)
			throw new IllegalStateException("ptrace(PTRACE_SYSCALL) failed");
	}

	public static void singlestep(int pid) throws IllegalStateException {
		if (ptracef(PTRACE_SINGLESTEP, pid, 0, 0) == -1)
			throw new IllegalStateException("ptrace(PTRACE_SINGLESTEP) failed");
		try {
			Thread.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void getregs(int pid, long regs) throws IllegalStateException {
		if (ptracef(PTRACE_GETREGS, pid, 0, regs) == -1)
			throw new IllegalStateException("ptrace(PTRACE_GETREGS) failed");
	}

	public static void setregs(int pid, long regs) throws IllegalStateException {
		if (ptracef(PTRACE_SETREGS, pid, 0, regs) == -1)
			throw new IllegalStateException("ptrace(PTRACE_SETREGS) failed");
	}

	public static long read_by_reg(int pid, int fieldoff) throws IllegalStateException {
		// offset of user_regs_struct + fieldoff
		return peektext(pid, peekuser(pid, fieldoff));
	}

	public static long read_by_reg_R(int pid, int fieldoff) throws IllegalStateException {
		// offset of user_regs_struct + fieldoff
		return Long.reverseBytes(peektext(pid, peekuser(pid, fieldoff)));
	}

	/*
	 * Returns value in user_regs_struct + offset.
	 */
	public static long peekuser(int pid, long offset) throws IllegalStateException {
		/*
		 * MemoryBuffer buf = new MemoryBuffer((int)offset + Long.BYTES);
		 * getregs(pid, Pointer.nativeValue(buf));
		 * return buf.getLong(offset);
		 */
		long data = ptracef(PTRACE_PEEKUSER, pid, offset, 0);
		if (data == -1)
			throw new IllegalStateException("ptrace(PTRACE_PEEKUSER) failed: " + offset);
		return data;
	}

	public static void pokeuser(int pid, long offset, long data) throws IllegalStateException {
		if (ptracef(PTRACE_POKEUSER, pid, offset, data) == -1)
			throw new IllegalStateException("ptrace(PTRACE_POKEUSER) failed");
	}

	/*
	 * Reads sizeof(long) bytes from addr.
	 */
	public static long peektext(int pid, long addr) throws IllegalStateException {
		long data = ptracef(PTRACE_PEEKTEXT, pid, addr, 0);
		// if (data == -1)
		//	throw new IllegalStateException("ptrace(PTRACE_PEEKTEXT) failed: " + addr);
		return data;
	}

	/*
	 * Writes data to addr in remote process.
	 */
	public static void poketext(int pid, long addr, long data) throws IllegalStateException {
		if (ptracef(PTRACE_POKETEXT, pid, addr, data) == -1)
			throw new IllegalStateException("ptrace(PTRACE_POKETEXT) failed");
	}

	public static MemoryBuffer read(int pid, long addr, int len) throws IllegalStateException {
		MemoryBuffer buf = Cacheable.buffer(len);
		for (int i = 0; i < len; i += Long.BYTES) {
			buf.setLong(i, peektext(pid, addr + i));
		}
		
		/*int bytesRead = 0;
		long read = 0;
		while (bytesRead < len) {
			read = peektext(pid, addr + bytesRead);
			buf.setByte(bytesRead, (byte) (read & 0xFF));
			bytesRead++;
		}*/

		return buf;
	}

	public static void read(int pid, long addr, Pointer buf, int len) throws IllegalStateException {
		for (int i = 0; i < len; i += Long.BYTES)
			buf.setLong(i, peektext(pid, addr + i));
	}

	public static void read(int pid, long addr, MemoryBuffer buf) throws IllegalStateException {
		read(pid, addr, buf, buf.size());
	}

	public static void write(int pid, long addr, Pointer buf, int len) throws IllegalStateException {
		for (int i = 0; i < len; i += Long.BYTES)
			poketext(pid, addr + i, buf.getLong(i));
	}

	public static void write(int pid, long addr, byte[] buf) throws IllegalStateException {
		for (int i = 0; i < buf.length; i += Long.BYTES)
			poketext(pid, addr + i, bytesToLong(buf, i));
	}

	public static void write(int pid, long addr, MemoryBuffer buf) throws IllegalStateException {
		write(pid, addr, buf, buf.size());
	}

	public static long bytesToLong(byte[] b, int offset) {
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result <<= 8;
			if (i < (b.length - offset)) {
				result |= (b[offset + i] & 0xFF);
			}
		}
		return result;
	}

	public static void setoptions(int pid, long flags) {
		if (ptracef(PTRACE_SETOPTIONS, pid, 0, flags) == -1)
			throw new IllegalStateException("ptrace(PTRACE_SETOPTIONS) failed");
	}

	public static class user_regs_struct extends Structure {
		
		public long r15;
		public long r14;
		public long r13;
		public long r12;
		public long rbp;
		public long rbx;
		public long r11;
		public long r10;
		public long r9;
		public long r8;
		public long rax;
		public long rcx;
		public long rdx;
		public long rsi;
		public long rdi;
		public long orig_rax;
		public long rip;
		public long cs;
		public long eflags;
		public long rsp;
		public long ss;
		public long fs_base;
		public long gs_base;
		public long ds;
		public long es;
		public long fs;
		public long gs;

		@Override
		protected List<String> getFieldOrder() {
			return createFieldsOrder("r15", "r14", "r13", "r12", "rbp", "rbx", "r11", "r10", "r9", "r8", "rax", "rcx", "rdx", "rsi", "rdi", "orig_rax", "rip", "cs", "eflags", "rsp", "ss", "fs_base", "gs_base", "ds", "es", "fs", "gs");
		}

		@Override
		public String toString() {
			return "user_regs_struct [r15=" + r15 + ", r14=" + r14 + ", r13=" + r13 + ", r12=" + r12 + ", rbp=" + rbp + ", rbx=" + rbx + ", r11=" + r11 + ", r10=" + r10 + ", r9=" + r9 + ", r8=" + r8 + ", rax=" + rax + ", rcx=" + rcx + ", rdx=" + rdx + ", rsi=" + rsi + ", rdi=" + rdi + ", orig_rax=" + orig_rax + ", rip=" + rip + ", cs=" + cs + ", eflags=" + eflags + ", rsp=" + rsp + ", ss=" + ss + ", fs_base=" + fs_base + ", gs_base=" + gs_base + ", ds=" + ds + ", es=" + es + ", fs=" + fs + ", gs=" + gs + "]";
		}
		
	}

	// /usr/include/linux/ptrace.h

	/*
	 * Indicate that the process making this request should be traced.
	 * All signals received by this process can be intercepted by its
	 * parent, and its parent can use the other `ptrace' requests.
	 */
	public static int PTRACE_TRACEME = 0;
	/* Return the word in the process's text space at address ADDR. */
	public static int PTRACE_PEEKTEXT = 1;
	/* Return the word in the process's data space at address ADDR. */
	public static int PTRACE_PEEKDATA = 2;
	/* Return the word in the process's user area at offset ADDR. */
	public static int PTRACE_PEEKUSER = 3;
	/* Write the word DATA into the process's text space at address ADDR. */
	public static int PTRACE_POKETEXT = 4;
	/* Write the word DATA into the process's data space at address ADDR. */
	public static int PTRACE_POKEDATA = 5;
	/* Write the word DATA into the process's user area at offset ADDR. */
	public static int PTRACE_POKEUSER = 6;
	/* Continue the process. */
	public static int PTRACE_CONT = 7;
	/* Kill the process. */
	public static int PTRACE_KILL = 8;
	/*
	 * Single step the process.
	 * This is not supported on all machines.
	 */
	public static int PTRACE_SINGLESTEP = 9;
	/*
	 * Get all general purpose registers used by a processes.
	 * This is not supported on all machines.
	 */
	public static int PTRACE_GETREGS = 12;
	/*
	 * Set all general purpose registers used by a processes.
	 * This is not supported on all machines.
	 */
	public static int PTRACE_SETREGS = 13;
	/*
	 * Get all floating point registers used by a processes.
	 * This is not supported on all machines.
	 */
	public static int PTRACE_GETFPREGS = 14;
	/*
	 * Set all floating point registers used by a processes.
	 * This is not supported on all machines.
	 */
	public static int PTRACE_SETFPREGS = 15;
	/* Attach to a process that is already running. */
	public static int PTRACE_ATTACH = 16;
	/* Detach from a process attached to with PTRACE_ATTACH. */
	public static int PTRACE_DETACH = 17;
	/*
	 * Get all extended floating point registers used by a processes.
	 * This is not supported on all machines.
	 */
	public static int PTRACE_GETFPXREGS = 18;
	/*
	 * Set all extended floating point registers used by a processes.
	 * This is not supported on all machines.
	 */
	public static int PTRACE_SETFPXREGS = 19;
	/* Continue and stop at the next (return from) syscall. */
	public static int PTRACE_SYSCALL = 24;
	/* Set ptrace filter options. */
	public static int PTRACE_SETOPTIONS = 0x4200;
	/* Get last ptrace message. */
	public static int PTRACE_GETEVENTMSG = 0x4201;
	/* Get siginfo for process. */
	public static int PTRACE_GETSIGINFO = 0x4202;
	/* Set new siginfo for process. */
	public static int PTRACE_SETSIGINFO = 0x4203;
	/* Get register content. */
	public static int PTRACE_GETREGSET = 0x4204;
	/* Set register content. */
	public static int PTRACE_SETREGSET = 0x4205;
	/*
	 * Like PTRACE_ATTACH, but do not force tracee to trap and do not affect
	 * signal or group stop state.
	 */
	public static int PTRACE_SEIZE = 0x4206;
	/* Trap seized tracee. */
	public static int PTRACE_INTERRUPT = 0x4207;
	/* Wait for next group event. */
	public static int PTRACE_LISTEN = 0x4208;
	public static int PTRACE_PEEKSIGINFO = 0x4209;
	public static int PTRACE_GETSIGMASK = 0x420a;
	public static int PTRACE_SETSIGMASK = 0x420b;
	public static int PTRACE_SECCOMP_GET_FILTER = 0x420c;

	/* Read signals from a shared (process wide) queue */
	public static int PTRACE_PEEKSIGINFO_SHARED = (1 << 0);

	/* Wait extended result codes for the above trace options. */
	public static int PTRACE_EVENT_FORK = 1;
	public static int PTRACE_EVENT_VFORK = 2;
	public static int PTRACE_EVENT_CLONE = 3;
	public static int PTRACE_EVENT_EXEC = 4;
	public static int PTRACE_EVENT_VFORK_DONE = 5;
	public static int PTRACE_EVENT_EXIT = 6;
	public static int PTRACE_EVENT_SECCOMP = 7;
	/* Extended result codes which enabled by means other than options. */
	public static int PTRACE_EVENT_STOP = 128;

	/* Options set using PTRACE_SETOPTIONS or using PTRACE_SEIZE @data param */
	public static int PTRACE_O_TRACESYSGOOD = 1;
	public static int PTRACE_O_TRACEFORK = (1 << PTRACE_EVENT_FORK);
	public static int PTRACE_O_TRACEVFORK = (1 << PTRACE_EVENT_VFORK);
	public static int PTRACE_O_TRACECLONE = (1 << PTRACE_EVENT_CLONE);
	public static int PTRACE_O_TRACEEXEC = (1 << PTRACE_EVENT_EXEC);
	public static int PTRACE_O_TRACEVFORKDONE = (1 << PTRACE_EVENT_VFORK_DONE);
	public static int PTRACE_O_TRACEEXIT = (1 << PTRACE_EVENT_EXIT);
	public static int PTRACE_O_TRACESECCOMP = (1 << PTRACE_EVENT_SECCOMP);

	/* eventless options */
	public static int PTRACE_O_EXITKILL = (1 << 20);
	public static int PTRACE_O_SUSPEND_SECCOMP = (1 << 21);

}