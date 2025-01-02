package com.Bili;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class NativeLibrary extends AbstractJni implements IOResolver {
    private final AndroidEmulator emulator;
    private final DvmClass LibBili;
    private final VM vm;

    public NativeLibrary() {
        emulator = AndroidEmulatorBuilder.for32Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .setProcessName("tv.danmaku.bili")
                .build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/bili/bilibili.apk"));
        vm.setJni(this);
        vm.setVerbose(true);
        // 打开多线程逻辑
        emulator.getBackend().registerEmuCountHook(10000); // 设置执行多少条指令切换一次线程
        emulator.getSyscallHandler().setVerbose(true);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);

        emulator.getSyscallHandler().addIOResolver(this);
        DalvikModule dm = vm.loadLibrary("bili", true);
        LibBili = vm.resolveClass("com.bilibili.nativelibrary.LibBili");
        dm.callJNI_OnLoad(emulator);
    }


    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("lilac open:" + pathname);
        if (pathname.equals("/proc/self/cmdline")) {
            return FileResult.success(new ByteArrayFileIO(
                            oflags,
                            pathname,
                            "tv.danmaku.bili\0".getBytes(StandardCharsets.UTF_8)
                    )
            );
        }
        return null;
    }


    public static void main(String[] args) {
        NativeLibrary nativeLibrary = new NativeLibrary();
        System.out.println(nativeLibrary.callS());
    }

    public String callS() {
        TreeMap<String, String> map = new TreeMap<>();
        map.put("build", "6180500");
        map.put("mobi_app", "android");
        map.put("channel", "shenma069");
        map.put("appkey", "1d8b6e7d45233436");
        map.put("s_locale", "zh_CN");
        DvmObject<?> mapObject = ProxyDvmObject.createObject(vm, map);
        String ret = LibBili.callStaticJniMethodObject(emulator, "s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;", mapObject).getValue().toString();
        return ret;
    }

    @Override
    public boolean callBooleanMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/Map->isEmpty()Z": {
                Map map = (Map) dvmObject.getValue();
                return map.isEmpty();
            }
        }
        return super.callBooleanMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/Map->get(Ljava/lang/Object;)Ljava/lang/Object;": {
                Map map = (Map) dvmObject.getValue();
                Object key = varArg.getObjectArg(0).getValue();
                return ProxyDvmObject.createObject(vm, map.get(key));
            }
            case "java/util/Map->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;": {
                Map map = (Map) dvmObject.getValue();
                Object key = varArg.getObjectArg(0).getValue();
                Object value = varArg.getObjectArg(1).getValue();
                return ProxyDvmObject.createObject(vm, map.put(key, value));
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/bilibili/nativelibrary/SignedQuery->r(Ljava/util/Map;)Ljava/lang/String;": {
                Map map = (Map) varArg.getObjectArg(0).getValue();
                return new StringObject(vm, SignedQuery.r(map));
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }


    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "com/bilibili/nativelibrary/SignedQuery-><init>(Ljava/lang/String;Ljava/lang/String;)V": {
                String arg1 = (String) varArg.getObjectArg(0).getValue();
                String arg2 = (String) varArg.getObjectArg(1).getValue();
                //todo: 这里虽然已经把SignedQuery类扣下来了，但是还是得用vm.resolveClass来构造，但是可以跟上真实的SignedQuery对象
                return vm.resolveClass("com/bilibili/nativelibrary/SignedQuery").newObject(new SignedQuery(arg1, arg2));
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }
}
