package com.example;

import com.github.unidbg.Emulator;
import com.github.unidbg.linux.ARM64SyscallHandler;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import unicorn.Arm64Const;

public class DemoARM64SyscallHandler extends ARM64SyscallHandler {
    public DemoARM64SyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    // 处理尚未模拟实现的系统调用


    @Override
    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        if (NR == 165) {
            getrusage(emulator);
            return true;
        }
        return super.handleUnknownSyscall(emulator, NR);
    }

    private void getrusage(Emulator<?> emulator) {
        // 获取X1的内容，等价于emulator.getContext().getPointerArg(1)
        UnidbgPointer rusage = UnidbgPointer.register(emulator, Arm64Const.UC_ARM64_REG_X1);
        byte[] rusageContent = hexStringToByteArray("00000000000000009f4a0b00000000000000000000000000c5e10100000000009052010000000000000000000000000000000000000000000000000000000000255e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d02000000000000d300000000000000");
        for (int i = 0; i < rusageContent.length; i++) {
            rusage.setByte(i, rusageContent[i]);
        }
    }

    /* s must be an even-length string. */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
