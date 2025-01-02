package com.dianping;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.github.unidbg.virtualmodule.android.JniGraphics;

import java.io.File;

public class NBridge extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmObject<?> SIUACollector;
    private final VM vm;

    public NBridge() {
        emulator = AndroidEmulatorBuilder
                .for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/dianping/dazhongdianping.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        // 使用libandroid.so的虚拟模块
        new AndroidModule(emulator, vm).register(memory);
        // 使用libjnigraphics.so的虚拟模块
        new JniGraphics(emulator, vm).register(memory);

        DalvikModule dm = vm.loadLibrary("mtguard", true);
        SIUACollector = vm.resolveClass("com/meituan/android/common/mtguard/NBridge$SIUACollector").newObject(null);
        dm.callJNI_OnLoad(emulator);
    }

    public static void main(String[] args) {
        NBridge nBridge = new NBridge();
//        System.out.println(nBridge.getEnvironmentInfo());
        System.out.println(nBridge.getEnvironmentInfoExtra());
    }

    @Override
    public DvmObject<?> allocObject(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "java/lang/StringBuilder->allocObject": {
                // 按照Unidbg中的源码，应该是这样补
                // return dvmClass.newObject(new StringBuilder());
                // 但是说过JDK的库用ProxyDvmObject.createObject更好
                return ProxyDvmObject.createObject(vm, new StringBuilder());
            }
        }
        return super.allocObject(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "java/lang/StringBuilder-><init>()V": {
                return;
            }
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->getEnvironmentInfo()Ljava/lang/String;": {
                return new StringObject(vm, "0|0|0|-|0|");
            }
            case "java/lang/StringBuilder->append(Ljava/lang/String;)Ljava/lang/StringBuilder;": {
                StringBuilder obj = (StringBuilder) dvmObject.getValue();
                String arg = (String) vaList.getObjectArg(0).getValue();
                return ProxyDvmObject.createObject(vm, obj.append(arg));
            }
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->isVPN()Ljava/lang/String;": {
                return new StringObject(vm, "0");
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> getObjectField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "com/meituan/android/common/mtguard/NBridge$SIUACollector->mContext:Landroid/content/Context;": {

            }
        }
        return super.getObjectField(vm, dvmObject, signature);
    }

    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("lilac open:" + pathname);
        return null;
    }

    public String getEnvironmentInfo() {
        String result = SIUACollector.callJniMethodObject(emulator, "getEnvironmentInfo()Ljava/lang/String;").getValue().toString();
        return result;
    }

    public String getEnvironmentInfoExtra() {
        String result = SIUACollector.callJniMethodObject(emulator, "getEnvironmentInfoExtra()Ljava/lang/String;").getValue().toString();
        return result;
    }

}
