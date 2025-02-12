package com.douyu;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.arm.backend.UnHook;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.utils.Inspector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DouYu extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public DouYu() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/douyu/douyu.apk"));
        vm.setVerbose(true);
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/douyu/libmakeurl2.5.0.so"), true);
        module = dm.getModule();
//        emulator.traceCode(module.base, module.base + module.size);
        dm.callJNI_OnLoad(emulator);
//        emulator.attach().addBreakPoint(module.base + 0x336C);
        // 开始地址是0x122d20d5
//        emulator.traceWrite(0x122d20a0 + 0x35, 0x122d20a0 + 0x35 + 0x20);   // 长度是32位所以是0x20
        hookStrCat();
        emulator.traceWrite(0xe4fff69bL, 0xe4fff69bL + 0x20);
        emulator.traceWrite(0xe4fff500L, 0xe4fff500L + 0x10);
        hookMemcpy();
    }

    public String getMakeUrl() {
        // args list
        List<Object> list = new ArrayList<>(10);
        // arg1 env
        list.add(vm.getJNIEnv());
        // arg2 jobject/jclass 用不到的话填0
        list.add(0);

        DvmObject<?> context = vm.resolveClass("android/content/Context").newObject(null);
        list.add(vm.addLocalObject(context));   // todo: 这个vm.addLocalObject是什么意思
        list.add(vm.addLocalObject(new StringObject(vm, "")));
        StringObject input3_1 = new StringObject(vm, "aid");
        StringObject input3_2 = new StringObject(vm, "client_sys");
        StringObject input3_3 = new StringObject(vm, "time");

        vm.addLocalObject(input3_1);
        vm.addLocalObject(input3_2);
        vm.addLocalObject(input3_3);

        list.add(vm.addLocalObject(new ArrayObject(input3_1, input3_2, input3_3)));

        StringObject input4_1 = new StringObject(vm, "android1");
        StringObject input4_2 = new StringObject(vm, "android");
        StringObject input4_3 = new StringObject(vm, "1638452332");

        list.add(vm.addLocalObject(new ArrayObject(input4_1, input4_2, input4_3)));

        StringObject input5_1 = new StringObject(vm, "");
        StringObject input5_2 = new StringObject(vm, "");
        StringObject input5_3 = new StringObject(vm, "");
        StringObject input5_4 = new StringObject(vm, "");
        StringObject input5_5 = new StringObject(vm, "");
        StringObject input5_6 = new StringObject(vm, "");
        StringObject input5_7 = new StringObject(vm, "");
        StringObject input5_8 = new StringObject(vm, "");
        StringObject input5_9 = new StringObject(vm, "");
        StringObject input5_10 = new StringObject(vm, "");
        StringObject input5_11 = new StringObject(vm, "");
        StringObject input5_12 = new StringObject(vm, "");
        StringObject input5_13 = new StringObject(vm, "");

        vm.addLocalObject(input5_1);
        vm.addLocalObject(input5_2);
        vm.addLocalObject(input5_3);
        vm.addLocalObject(input5_4);
        vm.addLocalObject(input5_5);
        vm.addLocalObject(input5_6);
        vm.addLocalObject(input5_7);
        vm.addLocalObject(input5_8);
        vm.addLocalObject(input5_9);
        vm.addLocalObject(input5_10);
        vm.addLocalObject(input5_11);
        vm.addLocalObject(input5_12);
        vm.addLocalObject(input5_13);

        list.add(vm.addLocalObject(
                new ArrayObject(
                        input5_1, input5_2, input5_3, input5_4, input5_5, input5_6, input5_7, input5_8,
                        input5_9, input5_10, input5_11, input5_12, input5_13
                )
        ));

        StringObject input6_1 = new StringObject(vm, "");
        StringObject input6_2 = new StringObject(vm, "");
        StringObject input6_3 = new StringObject(vm, "");
        StringObject input6_4 = new StringObject(vm, "");
        StringObject input6_5 = new StringObject(vm, "");
        StringObject input6_6 = new StringObject(vm, "");
        StringObject input6_7 = new StringObject(vm, "");
        StringObject input6_8 = new StringObject(vm, "");
        StringObject input6_9 = new StringObject(vm, "");
        StringObject input6_10 = new StringObject(vm, "");
        list.add(vm.addLocalObject(
                new ArrayObject(input6_1, input6_2, input6_3, input6_4, input6_5,
                        input6_6, input6_7, input6_8, input6_9, input6_10
                )
        ));

        list.add(0);
        list.add(1);
        // 参数准备完成
        // call function
        Number number = module.callFunction(emulator, 0x2f91, list.toArray());
        return vm.getObject(number.intValue()).getValue().toString();
    }

    public static void main(String[] args) {
        DouYu douYu = new DouYu();
//        douYu.traceLength();
        System.out.println("result: " + douYu.getMakeUrl());
    }

    public void traceLength() {
        emulator.getBackend().hook_add_new(new CodeHook() {
            int count = 0;

            @Override
            public void hook(Backend backend, long address, int size, Object user) {
                count += 1;
                System.out.println(count);
            }

            @Override
            public void onAttach(UnHook unHook) {

            }

            @Override
            public void detach() {

            }
        }, module.base, module.base + module.size, null);
    }


    public void hookStrCat() {
        emulator.attach().addBreakPoint(module.findSymbolByName("strcat", true).getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                UnidbgPointer r0 = emulator.getContext().getPointerArg(0);
                UnidbgPointer r1 = emulator.getContext().getPointerArg(1);
                System.out.println("strcat r0: " + r0);
                System.out.println("strcat arg0: " + r0.getString(0));
                System.out.println("strcat r1: " + r1);
                System.out.println("strcat arg1: " + r1.getString(0));
                System.out.println("===================================================");
                return true;
            }
        });
    }

    public void hookMemcpy() {
        emulator.attach().addBreakPoint(module.findSymbolByName("memcpy", true).getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                UnidbgPointer r0 = emulator.getContext().getPointerArg(0);
                UnidbgPointer r1 = emulator.getContext().getPointerArg(1);
                int arg2 = emulator.getContext().getIntArg(2);
                System.out.println("memcpy r0: " + r0);
                System.out.println("memcpy arg0: " + r0.getString(0));
                System.out.println("memcpy r1: " + r1);
                Inspector.inspect(r1.getByteArray(0, arg2), r1.toString());
                System.out.println("memcpy arg2: " + arg2);
                System.out.println("===================================================");
                return true;
            }
        });
    }
}
