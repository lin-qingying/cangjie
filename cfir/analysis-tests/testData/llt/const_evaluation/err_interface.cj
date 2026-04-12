// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {
    const func f(): Int64
    static const func f2(): Int64
}

const func g<T>(i: T) where T <: I {
    i.f() + T.f2()
}

class A {}

extend A <: I {
    public func f() {
        0
    }
    public static func f2() {
        1
    }
    public func f(a: Int64) {
        0
    }
}

main() {
    const a = A()
    g(a)
}

/* SCAN
error: non-constant function member 'f' cannot have the same name with 'const' function member in extended type
  ==> err_interface.cj:21:17:
   | 
21 |     public func f() {
   |                 ^ 
   | 
note: found 'const' function here
  ==> err_interface.cj:10:16:
   | 
10 |     const func f(): Int64
   |                ^ 
   | 

error: non-constant function member 'f2' cannot have the same name with 'const' function member in extended type
  ==> err_interface.cj:24:24:
   | 
24 |     public static func f2() {
   |                        ^^ 
   | 
note: found 'const' function here
  ==> err_interface.cj:11:23:
   | 
11 |     static const func f2(): Int64
   |                       ^^ 
   | 

error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_interface.cj:33:15:
   | 
33 |     const a = A()
   |               ^ 
   | 

3 errors generated, 3 errors printed.
*/
