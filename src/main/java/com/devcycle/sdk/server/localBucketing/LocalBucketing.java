package com.devcycle.sdk.server.localBucketing;

import java.util.Arrays;
import java.util.Collection;

import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;
import io.github.kawamuray.wasmtime.wasi.WasiCtxBuilder;


// https://github.com/kawamuray/wasmtime-java/blob/master/examples/src/main/java/examples/HelloWorld.java 

public final class LocalBucketing {
    public static void main(String[] args) {
        System.out.println("IM HERE");
        try (
                WasiCtx wasi = new WasiCtxBuilder().inheritStdout().inheritStderr().build();
                Store<Void> store = Store.withoutData(wasi);

                Engine engine = store.engine();
                Module module = Module.fromFile(engine, "./hello.wat");
                Func helloFunc = WasmFunctions.wrap(store, () -> {
                    System.err.println(">>> Calling back...");
                    System.err.println(">>> Hello World!");
                })
            ) {
            Collection<Extern> imports = Arrays.asList(Extern.fromFunc(helloFunc));
            try (Instance instance = new Instance(store, module, imports)) {
                try (Func f = instance.getFunc(store, "run").get()) {
                    WasmFunctions.Consumer0 fn = WasmFunctions.consumer(store, f);
                    fn.accept();
                }
            }
        }
    }
}




// import static io.github.kawamuray.wasmtime.WasmValType.I32;
// import static io.github.kawamuray.wasmtime.WasmValType.I64;

// import java.nio.ByteBuffer;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.concurrent.atomic.AtomicReference;

// import io.github.kawamuray.wasmtime.Extern;
// import io.github.kawamuray.wasmtime.Func;
// import io.github.kawamuray.wasmtime.Linker;
// import io.github.kawamuray.wasmtime.Memory;
// import io.github.kawamuray.wasmtime.Module;
// import io.github.kawamuray.wasmtime.Store;
// import io.github.kawamuray.wasmtime.WasmFunctions;
// import io.github.kawamuray.wasmtime.WasmFunctions.Consumer0;
// import io.github.kawamuray.wasmtime.wasi.WasiCtx;
// import io.github.kawamuray.wasmtime.wasi.WasiCtxBuilder;

// public class LocalBucketing {
//     // Build it with `cargo wasi build`
//     private static final String WASM_PATH = "./bucketing-lib.release.wasm";

//     public static void main(String[] args) {
//         User user = User.builder()
//             .userId("hi")
//             .build();
//         AtomicInteger counter = new AtomicInteger();

//         // Let the poll_word function to refer this as a placeholder of Memory because
//         // we have to add the function as import before loading the module exporting Memory.
//         AtomicReference<Memory> memRef = new AtomicReference<>();
        // try (WasiCtx wasi = new WasiCtxBuilder().inheritStdout().inheritStderr().build();
        //      Store<Void> store = Store.withoutData(wasi);
        //      Linker linker = new Linker(store.engine());
        //      Func pollWordFn = WasmFunctions.wrap(store, I64, I32, I32, (addr, len) -> {
        //          System.err.println("Address to store word: " + addr);
        //          ByteBuffer buf = memRef.get().buffer(store);
        //          String word = words[counter.getAndIncrement() % words.length];
        //          for (int i = 0; i < len && i < word.length(); i++) {
        //              buf.put(addr.intValue() + i, (byte) word.charAt(i));
        //          }
        //          return Math.min(word.length(), len);
        //      });
        //      Module module = Module.fromFile(store.engine(), WASM_PATH)) {

            // WasiCtx.addToLinker(linker);
            // linker.define("xyz", "poll_word", Extern.fromFunc(pollWordFn));
            // linker.module(store, "", module);

//             try (Memory mem = linker.get(store, "", "memory").get().memory();
//                  Func doWorkFn = linker.get(store, "", "do_work").get().func()) {
//                 memRef.set(mem);
//                 Consumer0 doWork = WasmFunctions.consumer(store, doWorkFn);
//                 doWork.accept();
//                 doWork.accept();
//                 doWork.accept();
//             }
//         }
//     }
// }
