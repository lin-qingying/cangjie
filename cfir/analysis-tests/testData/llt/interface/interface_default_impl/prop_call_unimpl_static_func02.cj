// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {
    static func foo(): Int64
    static func foo(a: Int64): Int64
    mut static prop a: Int64 {
        get() {
            foo()
        }
        set(x) {
            foo(x)
        }
    }
}

interface I2 <: I {}

main() {
    I2.a = 10
    I2.a
    0
}

/* SCAN
error: static invocation contains unimplemented static function 'foo'
  ==> prop_call_unimpl_static_func02.cj:25:5:
   | 
25 |     I2.a = 10
   |     ^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> prop_call_unimpl_static_func02.cj:14:13:
   | 
14 |             foo()
   |             ^^^ 
   | 

error: static invocation contains unimplemented static function 'foo'
  ==> prop_call_unimpl_static_func02.cj:26:5:
   | 
26 |     I2.a
   |     ^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> prop_call_unimpl_static_func02.cj:14:13:
   | 
14 |             foo()
   |             ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
