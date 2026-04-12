// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %noColor %f -o %n.%suffix 2>&1 | compare %f

interface I1 {
    static func f(): Unit {}
}

interface I2 <: I1 {
    static func f1(): Unit
    static func f(): Unit {}
}

func f<T>(): Unit where T <: I1 {
    T.f()
}

main() {
    f<I2>() // error
}

/* SCAN
error: can not instantiate 'Generics-T' by interface or abstract class 'Interface-I2' for it has unimplemented static member
  ==> generic_call_static_impl09.cj:23:7:
   | 
23 |     f<I2>() // error
   |       ^ 
   | 

1 error generated, 1 error printed.
*/
