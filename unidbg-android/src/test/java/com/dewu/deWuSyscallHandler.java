package com.dewu;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.context.EditableArm32RegisterContext;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.linux.file.DumpFileIO;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;
import unicorn.ArmConst;

import java.util.concurrent.ThreadLocalRandom;

public class deWuSyscallHandler extends ARM32SyscallHandler {
    public deWuSyscallHandler(SvcMemory svcMemory) {
        super(svcMemory);
    }

    @Override
    protected boolean handleUnknownSyscall(Emulator<?> emulator, int NR) {
        switch (NR) {
            case 190:
                vfork(emulator);
                return true;
            case 114:
                wait4(emulator);
                return true;
        }
        return super.handleUnknownSyscall(emulator, NR);
    }

    private void vfork(Emulator<?> emulator) {
        EditableArm32RegisterContext context = emulator.getContext();
        int childPid = emulator.getPid() + ThreadLocalRandom.current().nextInt(256);
        int r0 = childPid;
        System.out.println("vfork pid = " + r0);
        context.setR0(r0);
    }

    private void wait4(Emulator<?> emulator) {
        return;
    }

    @Override
    protected int pipe2(Emulator<?> emulator) {
        EditableArm32RegisterContext context = emulator.getContext();
        Pointer pipefd = context.getPointerArg(0);
        int write = getMinFd();
        this.fdMap.put(write, new DumpFileIO(write));
        int read = getMinFd();
        String command = emulator.get("command");
        System.out.println("fuck cmd:"+command);
        // stdout中写入popen command 应该返回的结果
        String stdout = "\n";
        if(command.equals("stat /data")){
            stdout = "  File: /data\n" +
                    "  Size: 4096     Blocks: 16      IO Blocks: 512 directory\n" +
                    "Device: 10305h/66309d    Inode: 2        Links: 53\n" +
                    "Access: (0771/drwxrwx--x)       Uid: ( 1000/  system)   Gid: ( 1000/  system)\n" +
                    "Access: 2022-04-22 16:08:42.656423789 +0800\n" +
                    "Modify: 1970-02-05 00:02:38.459999996 +0800\n" +
                    "Change: 1971-07-05 09:54:50.369999990 +0800";
        }
        this.fdMap.put(read, new ByteArrayFileIO(0, "pipe2_read_side", stdout.getBytes()));
        pipefd.setInt(0, read);
        pipefd.setInt(4, write);
        context.setR0(0);
        return 0;
    }

    @Override
    protected int clock_gettime(Backend backend, Emulator<?> emulator) {
        // 等价于：emulator.getContext().getIntArg(0)
        int clk_id = backend.reg_read(ArmConst.UC_ARM_REG_R0).intValue();
        Pointer tp = UnidbgPointer.register(emulator, ArmConst.UC_ARM_REG_R1);
        if (clk_id == 2) {
            tp.setInt(0, 0);
            tp.setInt(4, 1);
            return 0;
        }
        return super.clock_gettime(backend, emulator);
    }


}
