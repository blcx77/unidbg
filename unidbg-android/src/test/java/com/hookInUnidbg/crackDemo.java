package com.hookInUnidbg;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.hookzz.*;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;
import keystone.Keystone;
import keystone.KeystoneArchitecture;
import keystone.KeystoneEncoded;
import keystone.KeystoneMode;
import unicorn.ArmConst;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class crackDemo {

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    crackDemo() {
        emulator = AndroidEmulatorBuilder.for32Bit().build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/hookInUnidbg/app-debug.apk"));
        DalvikModule dm = vm.loadLibrary("hookinunidbg", true);
        module = dm.getModule();
//        emulator.attach().addBreakPoint(module.findSymbolByName("base64_encode").getAddress());
//        emulator.attach().addBreakPoint(module, "base64_encode", new BreakPointCallback() {
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                RegisterContext registerContext = emulator.getContext();
//                // 这个input指针是char*
//                UnidbgPointer input = registerContext.getPointerArg(0);
//                // 这个output指针是char*
//                final UnidbgPointer output = registerContext.getPointerArg(2);
//                int length = registerContext.getIntArg(1);
//                String inputString = input.getString(0);
//                String FakeName = "Hello World";
//                int FakeLength = FakeName.length();
//                // 给FakeName分配内存
//                MemoryBlock memoryBlock = memory.malloc(FakeLength, true);
//                memoryBlock.getPointer().write(FakeName.getBytes(StandardCharsets.UTF_8));
//                if (inputString.equals("lilac")) {
//                    emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, memoryBlock.getPointer().peer);
//                    emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R1, FakeLength);
//                }
//                // 设置OnLeave
//                emulator.attach().addBreakPoint(registerContext.getLRPointer().peer, new BreakPointCallback() {
//                    @Override
//                    public boolean onHit(Emulator<?> emulator, long address) {
//                        String result = output.getString(0);
//                        System.out.println("base64 result: " + result);
//                        return true;
//                    }
//                });
//                return true;
//            }
//        });
//        emulator.attach().addBreakPoint(module.findSymbolByName("verifyApkSign").getAddress(), new BreakPointCallback() {
//            RegisterContext registerContext = emulator.getContext();
//
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                System.out.println("替换函数 verifyApkSign");
//                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, registerContext.getLRPointer().peer);
//                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
//                return true;
//            }
//        });
//
//        emulator.attach().addBreakPoint(module, 0x08CA, new BreakPointCallback() {
//            RegisterContext registerContext = emulator.getContext();
//
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                System.out.println("nop这里的调用");
//                // 表示跳过当前指令
//                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_PC, registerContext.getPCPointer().peer + 4 + 1);
//                emulator.getBackend().reg_write(ArmConst.UC_ARM_REG_R0, 0);
//                return true;
//            }
//        });
//        int patchCode = 0x4FF00000;
//        emulator.getMemory().pointer(module.base + 0x08CA).setInt(0, patchCode);
//        try (Keystone keystone = new Keystone(KeystoneArchitecture.Arm, KeystoneMode.ArmThumb)) {
//            KeystoneEncoded encoded = keystone.assemble("mov r0,0");
//            byte[] patchCode = encoded.getMachineCode();
//            emulator.getMemory().pointer(module.base + 0x08CA).write(0, patchCode, 0, patchCode.length);
//        }
        IxHook ixHook = XHookImpl.getInstance(emulator);
        ixHook.register(module.name, "base64_encode", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, long originFunction) {
                String str = emulator.getContext().getPointerArg(0).getString(0);
                System.out.println("xHook base64 input: " + str);
                return HookStatus.RET(emulator, originFunction);
            }
        });
        ixHook.refresh();
        /////////////////////////////////////////////////////////////////////////////////////
        IHookZz hookZz = HookZz.getInstance(emulator);
        hookZz.enable_arm_arm64_b_branch();  // 测试enable_arm_arm64_b_branch，可有可无
        hookZz.wrap(module.findSymbolByName("base64_encode"), new WrapCallback<HookZzArm32RegisterContext>() {
            @Override
            public void preCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer pointer = ctx.getPointerArg(0);
//                UnidbgPointer pointer = ctx.getPointerArg(0);   // 直接改成UnidbgPointer也是没问题的
                int length = ctx.getIntArg(1);
                byte[] input = pointer.getByteArray(0, length);
                Inspector.inspect(input, "HookZz base64 input");
                // 把参数二存到ctx里，等到postCall即frida的OnLeave中再取出来
                ctx.push(ctx.getPointerArg(2));
            }

            @Override
            public void postCall(Emulator<?> emulator, HookZzArm32RegisterContext ctx, HookEntryInfo info) {
                Pointer result = ctx.pop();
                System.out.println("HookZz base64 result: " + result.getString(0));
            }
        });

        dm.callJNI_OnLoad(emulator);
    }


    public void call() {
        DvmClass dvmClass = vm.resolveClass("com/example/hookinunidbg/MainActivity");
        String methodSign = "call()V";
        DvmObject<?> dvmObject = dvmClass.newObject(null);
        dvmObject.callJniMethodObject(emulator, methodSign);
    }

    public static void main(String[] args) {
        crackDemo mydemo = new crackDemo();
        mydemo.call();
    }

}
