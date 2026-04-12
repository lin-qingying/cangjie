// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class Base {
    public func foo(a: Int32): Int32 {
        0
    }
    public static func foo2<X>(a: X): X {
        a
    }
    public static func foo(a: Int64): Int64 {
        a
    }
}

class Sub <: Base {
    public static func foo(a: Int16): Int16 {
        0
    }
}

struct B<T> {}

class A {
    public func f(a: (Base) -> Int64, b: Sub) {
        return a(Base()) + 1
    } //f1
    public func f(a: (Sub) -> Int64, b: Base) {
        return a(Sub()) + 2
    } //f2
    public func g(a: Base): Int64 {
        return 1
    } //g1
    public func g(a: Sub): Int64 {
        return 2
    } //g2
}

func x<T>(input: B<T>): B<T> {
    input
}

func y<T>(input: B<T>): B<T> {
    input
}

func test<T>(input: T): Int64 {
    let xv = x<T>
    let yv = y<T>
    xv(yv(B<Int64>()))
    return 0
}

main(): Int64 {
    var a = A()
    var i = a.f(a.g, Base())
    var f2: ((Sub) -> Int64, Base) -> Int64 = a.f
    var i2 = f2(a.g, Base())
    test<Int64>(1)
    return 0
}

/* SCAN
error: overloaded functions 'foo' cannot mix static and non-static
  ==> member_func_overload_1.cj:16:24:
   | 
16 |     public static func foo(a: Int64): Int64 {
   |                        ^^^ 
   | 
note: non-static function is here
  ==> member_func_overload_1.cj:10:17:
   | 
10 |     public func foo(a: Int32): Int32 {
   |                 ^^^ 
   | 

error: mismatched types
  ==> member_func_overload_1.cj:55:11:
   | 
55 |     xv(yv(B<Int64>()))
   |           ^^^^^^^^^^ expected 'Struct-B<Generics-T>', found 'Struct-B<Int64>'
   | 

error: ambiguous match for reference 'g'
  ==> member_func_overload_1.cj:61:17:
   | 
61 |     var i = a.f(a.g, Base())
   |                 ^ 
   | 

error: no matching function declaration for function call 'f'
  ==> member_func_overload_1.cj:61:15:
   | 
61 |     var i = a.f(a.g, Base())
   |               ^ 
   | 
note: found possible candidate
  ==> member_func_overload_1.cj:30:17:
   | 
30 |     public func f(a: (Base) -> Int64, b: Sub) {
   |                 ^ 
   | 
note: found possible candidate
  ==> member_func_overload_1.cj:33:17:
   | 
33 |     public func f(a: (Sub) -> Int64, b: Base) {
   |                 ^ 
   | 

error: ambiguous match for reference 'g'
  ==> member_func_overload_1.cj:63:17:
   | 
63 |     var i2 = f2(a.g, Base())
   |                 ^ 
   | 

error: static member 'foo' cannot have the same name with non-static member in parent class or interfaces
  ==> member_func_overload_1.cj:22:5:
   | 
22 |     public static func foo(a: Int16): Int16 {
   |     ^ 
   | 

6 errors generated, 6 errors printed.
*/
