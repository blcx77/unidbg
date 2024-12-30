package com.izuiyou;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class NetWork extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass NetCrypto;
    private final VM vm;

    public NetWork() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("cn.xiaochuankeji.tieba")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/zuiyou/right573.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        new AndroidModule(emulator, vm).register(memory);
        DalvikModule dm = vm.loadLibrary("net_crypto", true);
        NetCrypto = vm.resolveClass("com.izuiyou.network.NetCrypto");
        dm.callJNI_OnLoad(emulator);
    }

    public String callSign() {
        String arg1 = "hello world";
        byte[] arg2 = "V I 50".getBytes(StandardCharsets.UTF_8);
        String ret = NetCrypto.callStaticJniMethodObject(emulator, "sign(Ljava/lang/String;[B)Ljava/lang/String;", arg1, arg2).getValue().toString();
        return ret;
    }

    public static void main(String[] args) {
        NetWork nw = new NetWork();
        String result = nw.callSign();
        System.out.println("call s result:" + result);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "com/izuiyou/common/base/BaseApplication->getAppContext()Landroid/content/Context;":
                DvmObject<?> context = vm.resolveClass("cn/xiaochuankeji/tieba/AppController").newObject(null);
                return context;
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }


    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "cn/xiaochuankeji/tieba/AppController->getPackageManager()Landroid/content/pm/PackageManager;":
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            case "cn/xiaochuankeji/tieba/AppController->getPackageName()Ljava/lang/String;":
                String packageName = vm.getPackageName();
                if (packageName != null) {
                    return new StringObject(vm, packageName);
                }
                break;
            case "cn/xiaochuankeji/tieba/AppController->getClass()Ljava/lang/Class;": {
                return dvmObject.getObjectType();
            }
            case "java/lang/Class->getSimpleName()Ljava/lang/String;": {
                String className = ((DvmClass) dvmObject).getClassName();
                String[] name = className.split("/");
                return new StringObject(vm, name[name.length - 1]);
            }
            case "cn/xiaochuankeji/tieba/AppController->getFilesDir()Ljava/io/File;": {
                // todo: 为什么要这样???
                return vm.resolveClass("java/io/File").newObject("/data/data/cn.xiaochuankeji.tieba/files");
            }
            case "java/io/File->getAbsolutePath()Ljava/lang/String;": {
                // todo: 为什么要这样???
                return new StringObject(vm, dvmObject.getValue().toString());
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public boolean callStaticBooleanMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/os/Debug->isDebuggerConnected()Z": {
                return false;
            }
        }
        return super.callStaticBooleanMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callStaticIntMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "android/os/Process->myPid()I": {
                return emulator.getPid();
            }
        }
        return super.callStaticIntMethodV(vm, dvmClass, signature, vaList);
    }
}
