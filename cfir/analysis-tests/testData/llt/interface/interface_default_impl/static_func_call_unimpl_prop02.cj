// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {
    mut static prop a: Int64
    static func fset(): Unit {
        a = 1
    }
    static func fget(): Int64 {
        a
    }
}

interface I2 <: I {}

main() {
    I2.fset()
    I2.fget()
    0
}

/* SCAN
error: static invocation contains unimplemented static property 'a'
  ==> static_func_call_unimpl_prop02.cj:22:5:
   | 
22 |     I2.fset()
   |     ^^^^^^^ 
   | 
note: indirect use of unimplemented property reference here
  ==> static_func_call_unimpl_prop02.cj:12:9:
   | 
12 |         a = 1
   |         ^ 
   | 

error: static invocation contains unimplemented static property 'a'
  ==> static_func_call_unimpl_prop02.cj:23:5:
   | 
23 |     I2.fget()
   |     ^^^^^^^ 
   | 
note: indirect use of unimplemented property reference here
  ==> static_func_call_unimpl_prop02.cj:15:9:
   | 
15 |         a
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
