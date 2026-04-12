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

interface I1<K> <: I<K> {
    static func f1(a: K): Unit {
        println("I1")
    }
}

interface I2<K> <: I<K> {
    static func f1(a: Int32): Unit {
        println("I2")
    }
}

func f<T>(a: T): Unit {
    I1<T>.f2(a)

    I2<T>.f2(a)
}

main() {
    f<Int32>(Int32(1))

    I1<Int32>.f2(Int32(1))

    I2<Int32>.f2(Int32(1))
}

/* SCAN
error: generic instantiation 'f<Int32>' causes ambiguous function 'f1'
  ==> interface_static_impl_with_generic08.cj:35:5:
   | 
35 |     f<Int32>(Int32(1))
   |     ^^^^^^^^ 
   | 
note: found candidate
  ==> interface_static_impl_with_generic08.cj:10:17:
   | 
10 |     static func f1(a: T): Unit
   |                 ^^ 
   | 
note: found candidate
  ==> interface_static_impl_with_generic08.cj:23:17:
   | 
23 |     static func f1(a: Int32): Unit {
   |                 ^^ 
   | 

error: generic instantiation 'I2<Int32>' causes ambiguous function 'f1'
  ==> interface_static_impl_with_generic08.cj:39:5:
   | 
39 |     I2<Int32>.f2(Int32(1))
   |     ^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_static_impl_with_generic08.cj:10:17:
   | 
10 |     static func f1(a: T): Unit
   |                 ^^ 
   | 
note: found candidate
  ==> interface_static_impl_with_generic08.cj:23:17:
   | 
23 |     static func f1(a: Int32): Unit {
   |                 ^^ 
   | 

error: static invocation contains unimplemented static function 'f1'
  ==> interface_static_impl_with_generic08.cj:31:5:
   | 
31 |     I2<T>.f2(a)
   |     ^^^^^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> interface_static_impl_with_generic08.cj:12:9:
   | 
12 |         f1(a)
   |         ^^ 
   | 

error: static invocation contains unimplemented static function 'f1'
  ==> interface_static_impl_with_generic08.cj:39:5:
   | 
39 |     I2<Int32>.f2(Int32(1))
   |     ^^^^^^^^^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> interface_static_impl_with_generic08.cj:12:9:
   | 
12 |         f1(a)
   |         ^^ 
   | 

4 errors generated, 4 errors printed.
*/
