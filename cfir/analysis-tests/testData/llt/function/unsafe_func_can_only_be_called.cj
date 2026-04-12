// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %cmp_opt %f 2>&1 | compare %f

unsafe func unsafeFunc() {
    println("call unsafeFunc")
}

func funcWithFuncParam(fn: () -> Unit) {
    fn()
}

class ClassWithUnsafeMember {
    unsafe func unsafeFunc() {
        println("call unsafeFunc")
    }
}

main() {
    unsafe { unsafeFunc() } // unsafe function can only be called

    // normal reference
    var fn = unsafeFunc
    funcWithFuncParam(unsafeFunc)
    // member access
    let obj = ClassWithUnsafeMember()
    fn = obj.unsafeFunc
    funcWithFuncParam(obj.unsafeFunc)
    return 0
}

/* SCAN
[33mwarning[0m: '--error-count-limit' is specified multiple times. The last one is chosen.
[33mwarning[0m: '--diagnostic-format' is specified multiple times. The last one is chosen.
error: the unsafe function can only be called rather than as name reference
  ==> unsafe_func_can_only_be_called.cj:27:14:
   | 
27 |     var fn = unsafeFunc
   |              ^^^^^^^^^^ 
   | 

error: the unsafe function can only be called rather than as name reference
  ==> unsafe_func_can_only_be_called.cj:28:23:
   | 
28 |     funcWithFuncParam(unsafeFunc)
   |                       ^^^^^^^^^^ 
   | 

error: the unsafe function can only be called rather than as name reference
  ==> unsafe_func_can_only_be_called.cj:31:10:
   | 
31 |     fn = obj.unsafeFunc
   |          ^^^^^^^^^^^^^^ 
   | 

error: the unsafe function can only be called rather than as name reference
  ==> unsafe_func_can_only_be_called.cj:32:23:
   | 
32 |     funcWithFuncParam(obj.unsafeFunc)
   |                       ^^^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
