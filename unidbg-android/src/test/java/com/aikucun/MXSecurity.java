package com.aikucun;

import com.github.unidbg.*;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.Debugger;
import com.github.unidbg.debugger.FunctionCallListener;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unwind.Unwinder;
import unicorn.Arm64Const;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class MXSecurity extends AbstractJni {
    private final AndroidEmulator emulator;
    private final DvmClass security;
    private final VM vm;
    private final Module module;
    private TraceHook traceHook;

    public MXSecurity() {
        emulator = AndroidEmulatorBuilder.for64Bit()
                .addBackendFactory(new Unicorn2Factory(false))
                .setProcessName("com.aikucun.akapp")
                .build();
        emulator.getBackend().registerEmuCountHook(10000); // 设置执行多少条指令切换一次线程
        emulator.getSyscallHandler().setVerbose(true);
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/aikucun/com.aikucun.akapp_6.0.6_60006.apk"));
        vm.setVerbose(true);
        vm.setJni(this);
        DalvikModule dm = vm.loadLibrary("mx", true);
        module = dm.getModule();
        security = vm.resolveClass("com/mengxiang/arch/security/MXSecurity");
//        emulator.traceCode(module.base, module.base + module.size);
        dm.callJNI_OnLoad(emulator);
//        traceDigest();
        Debugger debugger = emulator.attach();
        debugger.traceFunctionCall(module, new FunctionCallListener() {
            @Override
            public void onCall(Emulator<?> emulator, long callerAddress, long functionAddress) {

            }

            @Override
            public void postCall(Emulator<?> emulator, long callerAddress, long functionAddress, Number[] args) {
                System.out.println("onCallFinish caller="
                        + UnidbgPointer.pointer(emulator, callerAddress)
                        + ", function=" + UnidbgPointer.pointer(emulator, functionAddress)
                );
            }
        });
        traceFunction();
    }

    public void callinit() {
        DvmObject<?> context = vm.resolveClass("com.aikucun.akapp.AppContext").newObject(null);
        int ret = security.callStaticJniMethodInt(emulator, "init", context, false);
        System.out.println("call init:" + ret);
    }

    public void callSignV1() {
//        emulator.traceCode(module.base, module.base + module.size);
        String arg1 = "https://zuul.aikucun.com/aggregation-center-facade/api/app/search/product/image/switch?" +
                "appid=38741001&did=6d2fe7c7702721c6b797cf22ec8f5f58&noncestr=4b373c&timestamp=1662394452&zuul=1";
        String arg2 = "4b373c";
        String arg3 = "1662394452";
        String result = security.callStaticJniMethodObject(emulator, "signV1", arg1, arg2, arg3).getValue().toString();
        System.out.println("call SignV1 result:" + result);
    }

    public static void main(String[] args) {
        MXSecurity mxSecurity = new MXSecurity();
        mxSecurity.callinit();
        mxSecurity.callSignV1();
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/aikucun/akapp/AppContext->getPackageManager()Landroid/content/pm/PackageManager;": {
                return vm.resolveClass("android/content/pm/PackageManager").newObject(null);
            }
            case "com/aikucun/akapp/AppContext->getPackageName()Ljava/lang/String;": {
                return new StringObject(vm, vm.getPackageName());
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    public void traceDigest() {
        long callAddr = module.base + 0xE53C;
        emulator.attach().addBreakPoint(callAddr, new BreakPointCallback() {
            String traceFile = "unidbg-android/src/test/java/com/aikucun/trace.log";
            PrintStream traceStream;

            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                try {
                    traceStream = new PrintStream(new FileOutputStream(traceFile), true);
                    traceHook = emulator.traceCode(module.base, module.base + module.size);
                    traceHook.setRedirect(traceStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        emulator.attach().addBreakPoint(callAddr + 4, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                traceHook.stopTrace();
                return true;
            }
        });
    }

    public void traceDigest2() {
        long callAddr = module.base + 0xd804;
        emulator.attach().addBreakPoint(callAddr, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext registerContext = emulator.getContext();
                traceHook = emulator.traceCode(module.base, module.base + module.size);
                emulator.attach().addBreakPoint(registerContext.getLR(), new BreakPointCallback() {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address) {
                        traceHook.stopTrace();
                        return true;
                    }
                });
                return true;
            }
        });
    }

    public void traceDigest3() {
        final long begin = module.base;
        final long end = module.base + module.size;
        final AssemblyCodeDumper hook = new AssemblyCodeDumper(emulator, begin, end, null);
        long funcAddr = module.base + 0xd804;
        emulator.attach().addBreakPoint(funcAddr, new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                RegisterContext registerContext = emulator.getContext();
                emulator.getBackend().hook_add_new(hook, begin, end, this);
                emulator.attach().addBreakPoint(registerContext.getLR(), new BreakPointCallback() {
                    @Override
                    public boolean onHit(Emulator<?> emulator, long address) {
                        hook.stopTrace();
                        return true;
                    }
                });
                return true;
            }
        });
    }

    public void traceFunction() {
        Debugger debugger = emulator.attach();
        PrintStream traceStream = null;
        String traceFile = "unidbg-android/src/test/java/com/aikucun/traceFunctions.txt";
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
