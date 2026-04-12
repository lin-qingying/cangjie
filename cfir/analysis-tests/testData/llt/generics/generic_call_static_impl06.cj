// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %noColor %f -o %n.%suffix 2>&1 | compare %f

interface I {
    static func f1(): Unit
}

abstract class A <: I {}

func f<T>(): Unit where T <: A {
    T.f1()
}

main() {
    f<A>()
}

/* SCAN
error: abstract class 'A' cannot contain unimplemented static function 'f1'
  ==> generic_call_static_impl06.cj:13:16:
   | 
13 | abstract class A <: I {}
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
