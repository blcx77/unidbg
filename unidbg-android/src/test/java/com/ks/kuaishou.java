package com.ks;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.ARM32SyscallHandler;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;

public class kuaishou extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmClass Watermelon;
    private final VM vm;

    public kuaishou() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setRootDir(new File("unidbg-android/src/test/resources/ks/rootfs"))
                .setProcessName("com.smile.gifmaker")
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/ks/快手_10.3.40.25268.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        emulator.getSyscallHandler().addIOResolver(this);

        DalvikModule dm = vm.loadLibrary("ksse", true);
        Watermelon = vm.resolveClass("com.kuaishou.dfp.envdetect.jni.Watermelon");
        dm.callJNI_OnLoad(emulator);
    }


    public String call() {
        return Watermelon.newObject(null).callJniMethodObject(emulator, "jniCommand(ILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", 1114128, null, null, null).getValue().toString();
    }

    public static void main(String[] args) {
        kuaishou ks = new kuaishou();
        System.out.println(ks.call());
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("file open:" + pathname);
        return null;
    }
}
