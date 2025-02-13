package com.lamoda;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.debugger.BreakPointCallback;
import com.github.unidbg.debugger.FunctionCallListener;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.api.SystemService;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unwind.Unwinder;
import com.github.unidbg.utils.Inspector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Signer extends AbstractJni implements IOResolver {
    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
        System.out.println("file open:" + pathname);
        if (pathname.equals("/dev/urandom")) {
            emulator.getUnwinder().unwind();

        }
        return null;
    }

    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;
    private final DvmObject NativeLibHelper;

    Signer() {
        // 创建模拟器实例
        emulator = AndroidEmulatorBuilder
                .for64Bit() // 我选择分析 ARM64 的 SO
                .setProcessName("com.lamoda.lite") // 传入进程名
                .build();
        // 获取模拟器的内存操作接口
        final Memory memory = emulator.getMemory();
        // 设置系统类库解析
        memory.setLibraryResolver(new AndroidResolver(23));
        // 创建Android虚拟机,传入APK，Unidbg可以替我们做部分签名校验的工作
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/lamodo/com.lamoda.lite@0@.apk"));
        vm.setJni(this); // 设置JNI
        vm.setVerbose(true); // 打印日志
        emulator.getSyscallHandler().addIOResolver(this);// 设置文件处理器

//        traceCode();
//        traceFunction();
        DalvikModule dm = vm.loadLibrary("signer", true); // 加载 libsigner.so，Unidbg 会到 apk 的 lib/arm64-v8a 下寻找。
        module = dm.getModule(); //获取目标模块的句柄


        dm.callJNI_OnLoad(emulator); // 调用目标 SO 的 JNI_OnLoad
        // 构造调用目标函数的对象
//        traceFunction();
        hookMemcpy();
//        emulator.traceWrite(0xe4fff330L, 0xe4fff330L);
//        emulator.traceWrite(0x12089110L, 0x12089110L);
//        emulator.attach().addBreakPoint(module.base+0xb80);
//        emulator.attach().addBreakPoint(module.base + 0x1A24);
//        emulator.attach().addBreakPoint(module.base + 0x7AEDC);   // 取其arg0
//        emulator.attach().addBreakPoint(module.base + 0x7B278);   // 取X20
//        emulator.attach().addBreakPoint(module.base + 0x1DBC, new BreakPointCallback() {
//            @Override
//            public boolean onHit(Emulator<?> emulator, long address) {
//                System.out.println("0x1DBC这个地址调用了一次");
//                return true;
//            }
//        });
//        emulator.traceWrite(0xe4fff270L, 0xe4fff270L);
//        emulator.traceWrite(0xbffff270L, 0xbffff270L);
        emulator.traceWrite(0xe4fff250L, 0xe4fff250L);
        NativeLibHelper = vm.resolveClass("com.adjust.sdk.sig.NativeLibHelper").newObject(null);
    }

    public byte[] callNsign() {
        // arg1
        DvmObject context = vm.resolveClass("com.lamoda.lite.Application", vm.resolveClass("android/content/Context")).newObject(null);
        // arg2
        Map<String, String> map = new HashMap<>();
        map.put("api_level", "29");
        map.put("event_buffering_enabled", "0");
        map.put("hardware_name", "QKQ1.190828.002 test-keys");
        map.put("partner_params", "{\"partner_id\":\"lamodaby\"}");
        map.put("app_version", "4.25.0");
        map.put("app_token", "bnrkfzymzyjp");
        map.put("event_count", "11");
        map.put("session_length", "1018");
        map.put("created_at", "2023-04-02T12:43:23.486Z+0800");
        map.put("device_type", "phone");
        map.put("language", "ru");
        map.put("connectivity_type", "1");
        map.put("mcc", "460");
        map.put("device_manufacturer", "Xiaomi");
        map.put("display_width", "1080");
        map.put("event_token", "oapsei");
        map.put("time_spent", "1014");
        map.put("device_name", "MIX 2S");
        map.put("needs_response_details", "1");
        map.put("os_build", "QKQ1.190828.002");
        map.put("cpu_type", "arm64-v8a");
        map.put("screen_size", "normal");
        map.put("screen_format", "long");
        map.put("subsession_count", "2");
        map.put("secret_id", "4");
        map.put("mnc", "01");
        map.put("os_version", "10");
        map.put("callback_params", "{\"uid\":\"AE024064EF032964D3106E5402F60208\",\"device_model\":\"MIX 2S\",\"app_version\":\"4.25.0\",\"lid\":\"ZEACrmQpA+9UbhDTCAL2AgA=\",\"device_group\":\"Phone\",\"shop_country\":\"BY\",\"display_size\":\"5,2\",\"device_manufacturer\":\"Xiaomi\"}");
        map.put("android_uuid", "b124840b-e2bc-4c96-afad-5c1f13aa8ed8");
        map.put("environment", "production");
        map.put("screen_density", "high");
        map.put("attribution_deeplink", "1");
        map.put("session_count", "1");
        map.put("display_height", "2030");
        map.put("package_name", "com.lamoda.lite");
        map.put("os_name", "android");
        map.put("android_id", "9b8f038015568dfb");
        map.put("app_secret", "187957353611902444387878081751625741774");
        map.put("ui_mode", "1");
        map.put("activity_kind", "event");
        map.put("client_sdk", "android4.33.2");
        DvmObject mapArg = ProxyDvmObject.createObject(vm, map);
        // arg3
        ByteArray barr = new ByteArray(vm, hexStringToByteArray("d9f19cc21bf1c952f4c244b4a2ceaafa388e9d3d6eb90ec618d01bee4bb79424"));
        // arg4
        int i = 29;
        byte[] ret = (byte[]) NativeLibHelper.callJniMethodObject(emulator, "nSign", context, mapArg, barr, i).getValue();
        Inspector.inspect(ret, "result");
        return ret;
    }

    public void traceCode() {
        String traceFile = "unidbg-android/src/test/resources/lamodo/trace/tracecode.txt";
        PrintStream traceStream;
        try {
            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
            emulator.traceCode(module.base, module.base + module.size).setRedirect(traceStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void traceFunction() {
        String traceFile = "unidbg-android/src/test/resources/lamodo/trace/tracefunction.txt";
        try {
            final PrintStream traceStream;
            traceStream = new PrintStream(new FileOutputStream(traceFile), true);
            emulator.attach().traceFunctionCall(module, new FunctionCallListener() {
                @Override
                public void onCall(Emulator<?> emulator, long callerAddress, long functionAddress) {
                }

                @Override
                public void postCall(Emulator<?> emulator, long callerAddress, long functionAddress, Number[] args) {
                    traceStream.println("onCallFinish caller=" + UnidbgPointer.pointer(emulator, callerAddress) + ", function=" + UnidbgPointer.pointer(emulator, functionAddress));
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "java/security/MessageDigest->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;": {
                StringObject type = varArg.getObjectArg(0);
                assert type != null;
                try {
                    return vm.resolveClass("java/security/MessageDigest").newObject(MessageDigest.getInstance(type.getValue()));
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                }
            }
            case "java/security/KeyStore->getInstance(Ljava/lang/String;)Ljava/security/KeyStore;": {
                String type = (String) varArg.getObjectArg(0).getValue();
                return vm.resolveClass("java/security/KeyStore").newObject(null);
            }
            case "javax/crypto/Mac->getInstance(Ljava/lang/String;)Ljavax/crypto/Mac;": {
                String algorithm = varArg.getObjectArg(0).getValue().toString();
                try {
                    return vm.resolveClass("javax/crypto/Mac").newObject(Mac.getInstance(algorithm));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    @Override
    public void callVoidMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/security/MessageDigest->update([B)V": {
                MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
                ByteArray array = varArg.getObjectArg(0);
                assert array != null;
                messageDigest.update(array.getValue());
                return;
            }
            case "android/view/Display->getMetrics(Landroid/util/DisplayMetrics;)V": {
                return;
            }
            case "java/security/KeyStore->load(Ljava/security/KeyStore$LoadStoreParameter;)V": {
                return;
            }
            case "javax/crypto/Mac->init(Ljava/security/Key;)V": {
                return;
            }
            case "javax/crypto/Mac->update([B)V": {
                return;
            }
        }
        super.callVoidMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> callObjectMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/security/MessageDigest->digest()[B": {
                MessageDigest messageDigest = (MessageDigest) dvmObject.getValue();
                byte[] ret = messageDigest.digest();
                return new ByteArray(vm, ret);
            }
            case "android/content/Context->getSystemService(Ljava/lang/String;)Ljava/lang/Object;": {
                StringObject serviceName = varArg.getObjectArg(0);
                assert serviceName != null;
                return new SystemService(vm, serviceName.getValue());
            }
            case "android/hardware/SensorManager->getSensorList(I)Ljava/util/List;": {
                return new ArrayListObject(vm, Collections.<DvmObject<?>>emptyList());
            }
            case "android/view/WindowManager->getDefaultDisplay()Landroid/view/Display;": {
                return vm.resolveClass("android/view/Display").newObject(signature);
            }
            case "java/security/KeyStore->getKey(Ljava/lang/String;[C)Ljava/security/Key;": {
                return vm.resolveClass("java/security/Key").newObject(null);
            }
            case "java/util/HashMap->toString()Ljava/lang/String;": {
                HashMap hashMap = (HashMap) dvmObject.getValue();
                return new StringObject(vm, hashMap.toString());
            }
            case "java/lang/String->getBytes()[B": {
                String str = (String) dvmObject.getValue();
                return new ByteArray(vm, str.getBytes());
            }
            case "javax/crypto/Mac->doFinal()[B": {
                return new ByteArray(vm, hexStringToByteArray("d9f19cc21bf1c952f4c244b4a2ceaafa388e9d3d6eb90ec618d01bee4bb79424"));
            }
            case "java/util/HashMap->get(Ljava/lang/Object;)Ljava/lang/Object;": {
                HashMap hashMap = (HashMap) dvmObject.getValue();
                String key = varArg.getObjectArg(0).getValue().toString();
                System.out.println("key:" + key);
                Object value = hashMap.get(key);
                if (value != null) {
                    return new StringObject(vm, value.toString());
                } else {
                    return null;
                }
            }
            case "java/util/HashMap->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;": {
                HashMap hashMap = new HashMap();
                String key = varArg.getObjectArg(0).getValue().toString();
                String value = varArg.getObjectArg(1).getValue().toString();
                hashMap.put(key, value);
                return dvmObject;
            }
        }
        return super.callObjectMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "android/hardware/Sensor->TYPE_ALL:I": {
                return -1;
            }
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public int callIntMethod(BaseVM vm, DvmObject<?> dvmObject, String signature, VarArg varArg) {
        switch (signature) {
            case "java/util/List->size()I": {
                List list = (List) dvmObject.getValue();
                return list.size();
            }
        }
        return super.callIntMethod(vm, dvmObject, signature, varArg);
    }

    @Override
    public DvmObject<?> newObject(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        switch (signature) {
            case "android/util/DisplayMetrics-><init>()V": {
                return dvmClass.newObject(signature);
            }
        }
        return super.newObject(vm, dvmClass, signature, varArg);
    }

    @Override
    public int getIntField(BaseVM vm, DvmObject<?> dvmObject, String signature) {
        switch (signature) {
            case "android/util/DisplayMetrics->widthPixels:I": {
                return 1080;
            }
            case "android/util/DisplayMetrics->heightPixels:I": {
                return 2160;
            }
        }
        return super.getIntField(vm, dvmObject, signature);
    }

    public static void main(String[] args) {
        Logger.getLogger(DalvikVM64.class).setLevel(Level.DEBUG);
        Signer signer = new Signer();
        signer.callNsign();
    }

    public void hookMemcpy() {
        emulator.attach().addBreakPoint(module.findSymbolByName("memcpy", true).getAddress(), new BreakPointCallback() {
            @Override
            public boolean onHit(Emulator<?> emulator, long address) {
                UnidbgPointer src = emulator.getContext().getPointerArg(1);
                int n = emulator.getContext().getIntArg(2);
                Inspector.inspect(src.getByteArray(0, n), "memcpy source:" + src);
                return true;
            }
        });
    }

}