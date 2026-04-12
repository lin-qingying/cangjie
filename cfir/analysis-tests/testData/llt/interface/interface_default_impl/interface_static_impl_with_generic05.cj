// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I<T> {
    static func f1(a: T): Unit

    static func f3(a: T): Unit {
        f1(a)
    }
}

interface I1 <: I<Int64> & I<String> {
    static func f1(a: Int64): Unit {
        f1("1")
    }
    static func f1(a: String): Unit {
        f2()
    }
    static func f2(): Unit
}

main() {
    I1.f3(1)
}

/* SCAN
error: static invocation contains unimplemented static function 'f2'
  ==> interface_static_impl_with_generic05.cj:28:5:
   | 
28 |     I1.f3(1)
   |     ^^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> interface_static_impl_with_generic05.cj:22:9:
   | 
22 |         f2()
   |         ^^ 
   | 

1 error generated, 1 error printed.
*/
