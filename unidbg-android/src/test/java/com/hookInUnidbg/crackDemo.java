package com.hookInUnidbg;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

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
        emulator.attach().addBreakPoint(module.findSymbolByName("base64_encode").getAddress());
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
