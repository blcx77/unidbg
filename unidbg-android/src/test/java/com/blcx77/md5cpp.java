package com.blcx77;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.debugger.FunctionCallListener;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class md5cpp extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public md5cpp() {
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .addBackendFactory(new Unicorn2Factory(true))
                .build();

        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();
        vm.setJni(this);
        vm.setVerbose(true);
        DalvikModule dm = vm.loadLibrary(new File("unidbg-android/src/test/resources/blcx77/libsomd5.so"), true);
        module = dm.getModule();
        dm.callJNI_OnLoad(emulator);
//        traceFunction();
    }

    public String call() {
        DvmClass dvmClass = vm.resolveClass("com/example/somd5/MainActivity");
        String methodSign = "md5FromCpp()Ljava/lang/String;";
        DvmObject<?> dvmObject = dvmClass.newObject(null);
        return dvmObject.callJniMethodObject(emulator, methodSign).toString();
    }

    public static void main(String[] args) {
        md5cpp md5 = new md5cpp();
        System.out.println(md5.call());
    }

    public void traceFunction() {
        Debugger debugger = emulator.attach();
        PrintStream traceStream = null;
        String traceFile = "unidbg-android/src/test/java/com/blcx77/traceFunctions.txt";
        try {
            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        final PrintStream finalTraceStream = traceStream;
        assert finalTraceStream != null;
        debugger.traceFunctionCall(null, new FunctionCallListener() {
            @Override
            public void onCall(Emulator<?> emulator, long callerAddress, long functionAddress) {
                int level = emulator.getUnwinder().depth();
                for (int i = 0; i < level; i++) {
                    finalTraceStream.print("    |    ");
                }
                finalTraceStream.println("  " + "sub_" + Integer.toHexString((int) (functionAddress - module.base)) + "  ");
            }

            @Override
            public void postCall(Emulator<?> emulator, long callerAddress, long functionAddress, Number[] args) {

            }
        });

    }
}
