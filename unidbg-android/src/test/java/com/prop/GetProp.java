package com.prop;


import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.SystemPropertyHook;
import com.github.unidbg.linux.android.SystemPropertyProvider;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class GetProp extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass MainActivity;
    private final VM vm;

    public GetProp() {
        emulator = AndroidEmulatorBuilder.for64Bit().addBackendFactory(new Unicorn2Factory(true)).build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/getprop/app-debug.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

        //
        SystemPropertyHook systemPropertyHook = new SystemPropertyHook(emulator);
        systemPropertyHook.setPropertyProvider(new SystemPropertyProvider() {

            @Override
            public String getProperty(String key) {
                switch (key) {
                    case "ro.build.id": {
                        return "get id";
                    }
                    case "ro.build.version.sdk": {
                        return "get sdk";
                    }
                }
                return null;
            }
        });
        memory.addHookListener(systemPropertyHook);
        //
        DalvikModule dm = vm.loadLibrary("getprop", true);
        MainActivity = vm.resolveClass("com.example.getprop.MainActivity");
        dm.callJNI_OnLoad(emulator);
    }

    public String call() {
        return MainActivity.newObject(null).callJniMethodObject(emulator, "stringFromJNI").getValue().toString();
    }

    public static void main(String[] args) {
        GetProp getProp = new GetProp();
        getProp.call();
    }
}
