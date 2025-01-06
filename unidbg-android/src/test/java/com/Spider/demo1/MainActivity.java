package com.Spider.demo1;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.StandardCharsets;

// 验证std::string的内存模型
public class MainActivity extends AbstractJni {
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    MainActivity() {
        emulator = AndroidEmulatorBuilder.for32Bit().build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/Spider/demo1/app-debug.apk"));
        DalvikModule dm = vm.loadLibrary("sotest", true);
        module = dm.getModule();
        dm.callJNI_OnLoad(emulator);

        emulator.attach().addBreakPoint(module.base + 0x8E00, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext registerContext = emulator.getContext();
                UnidbgPointer arg1 = registerContext.getPointerArg(1);
                System.out.println("aaa => " + readStdString(arg1));
                return false;
            }
        });

    }

    public String readStdString(Pointer strptr) {
        boolean isTiny = (strptr.getByte(0) & 1) == 0;
        if (isTiny) {
            return strptr.getString(1);
        }
        return strptr.getPointer(emulator.getPointerSize() * 2L).getString(0);
    }

    public void writeStdString(Pointer strptr, String content) {
        boolean isTiny = (strptr.getByte(0) & 1) == 0;
        if (isTiny) {
            strptr.write(1, content.getBytes(StandardCharsets.UTF_8), 0, content.length());
        } else {
            strptr.getPointer(emulator.getPointerSize() * 2L).write(
                    0, content.getBytes(StandardCharsets.UTF_8), 0, content.length()
            );
        }
    }


    public String call() {
        DvmClass dvmClass = vm.resolveClass("com/example/sotest/MainActivity");
        String methodSign = "stringFromJNI()[java/lang/String;";
        DvmObject<?> dvmObject = dvmClass.newObject(null);
        return dvmObject.callJniMethodObject(emulator, methodSign).getValue().toString();
    }

    public static void main(String[] args) {
        MainActivity m = new MainActivity();
        System.out.println(m.call());

    }
}
