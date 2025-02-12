package com.examplemd5;

import com.example.DemoARM64SyscallHandler;
import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.linux.android.AndroidARM64Emulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.unix.UnixSyscallHandler;

import java.io.File;
import java.security.SecureRandom;

public class Demo extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public Demo() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/examplemd5/libenc.so"), true);
        module = dm.getModule();
        dm.callJNI_OnLoad(emulator);
    }

    public void call() {
        module.callFunction(emulator, 0x0961C, vm.getJNIEnv());
    }

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.call();
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            // ByteArray array = varArg.getObjectArg(0);
            //                return new StringObject(vm, new String(array.getValue()));
            case "java/security/SecureRandom-><init>()V": {
                SecureRandom secureRandom = new SecureRandom();
                System.out.println("java/security/SecureRandom-><init>()V  => " + secureRandom.hashCode());
                return ProxyDvmObject.createObject(vm, secureRandom);
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/security/SecureRandom->nextInt(I)I": {
                SecureRandom secureRandom = (SecureRandom) dvmObject.getValue();
                int result = secureRandom.nextInt(varArg.getIntArg(0));
                System.out.println("result => " + result);
                return result;
            }
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }
}