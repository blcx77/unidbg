package com.Bili;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;

import java.io.File;
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
        System.out.println("lilac open:"+pathname);
        return null;
    }


    public static void main(String[] args) {
        NativeLibrary nativeLibrary = new NativeLibrary();

    }

    public String callS(){
        TreeMap<String, String> map = new TreeMap<>();
        map.put("build", "6180500");
        map.put("mobi_app", "android");
        map.put("channel", "shenma069");
        map.put("appkey", "1d8b6e7d45233436");
        map.put("s_locale", "zh_CN");
        String ret = LibBili.callStaticJniMethodObject(emulator, "s(Ljava/util/SortedMap;)Lcom/bilibili/nativelibrary/SignedQuery;", map).getValue().toString();
        return ret;
    }

}
