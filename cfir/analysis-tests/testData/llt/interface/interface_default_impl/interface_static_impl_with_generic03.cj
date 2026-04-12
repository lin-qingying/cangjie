// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I<T> {
    static func f1(a: T): Unit
    static func f2(a: T): Unit {
        f1(a)
    }
}

interface I2 <: I<Int64> & I<String> {
    static func f1(a: Int64): Unit {
        println("Int64 f1 ${a}")
    }
}

main() {
    I2.f2("321")
}

/* SCAN
error: static invocation contains unimplemented static function 'f1'
  ==> interface_static_impl_with_generic03.cj:23:5:
   | 
23 |     I2.f2("321")
   |     ^^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> interface_static_impl_with_generic03.cj:12:9:
   | 
12 |         f1(a)
   |         ^^ 
   | 

1 error generated, 1 error printed.
*/
