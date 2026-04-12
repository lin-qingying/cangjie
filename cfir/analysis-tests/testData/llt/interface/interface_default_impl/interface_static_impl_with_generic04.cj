// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I<T> {
    static func f1(a: T): Unit
}

interface I1<T> <: I<String> {
    static func f3(a: T) {
        f1("123")
    }
}

interface I2 <: I1<Int64> & I1<String> {
    static func f1(a: Int64): Unit {
        println("Int64 f1 ${a}")
    }
}

main() {
    I2.f3(1)
}

/* SCAN
error: static invocation contains unimplemented static function 'f1'
  ==> interface_static_impl_with_generic04.cj:26:5:
   | 
26 |     I2.f3(1)
   |     ^^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> interface_static_impl_with_generic04.cj:15:9:
   | 
15 |         f1("123")
   |         ^^ 
   | 

1 error generated, 1 error printed.
*/
