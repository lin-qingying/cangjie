// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {}

interface I1<T> {}

enum E<T> <: I {
    EE
}

enum E1<T> <: I1<Int32> {
    EE1
}

main(): Int64 {
    let a: I = EE
    let b: I1<Int32> = EE1
    let c: I1<Int64> = EE1
    let d: I1<Int64> = EE1<Int64>
    return 1
}

/* SCAN
error: unable to infer the type of this expression, please add type annotation
  ==> enum_interface_box_02.cj:22:16:
   | 
22 |     let a: I = EE
   |                ^^ 
   | 

error: unable to infer the type of this expression, please add type annotation
  ==> enum_interface_box_02.cj:23:24:
   | 
23 |     let b: I1<Int32> = EE1
   |                        ^^^ 
   | 

error: unable to infer the type of this expression, please add type annotation
  ==> enum_interface_box_02.cj:24:24:
   | 
24 |     let c: I1<Int64> = EE1
   |                        ^^^ 
   | 

error: mismatched types
  ==> enum_interface_box_02.cj:25:24:
   | 
25 |     let d: I1<Int64> = EE1<Int64>
   |                        ^^^^^^^^^^ expected 'Interface-I1<Int64>', found 'Enum-E1<Int64>'
   | 

4 errors generated, 4 errors printed.
*/
