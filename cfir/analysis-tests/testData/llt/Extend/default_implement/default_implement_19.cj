// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {
    func foo(): Unit {
        println(1)
    }
    static func goo(): Unit {
        println(11)
    }
}

interface I2 <: I1 {
    func foo(): Unit {
        println(2)
    }
    static func goo(): Unit {
        println(22)
    }
}

class A {}

extend A <: I1 {}

extend A <: I2 {
    public func foo(): Unit {
        println(3)
    } // error
    public static func goo(): Unit {
        println(33)
    } // error
}

class B {}

extend B <: I1 {}

extend B {
    public func foo(): Unit {
        println(4)
    } // error
    public static func goo(): Unit {
        println(44)
    } // error
}

main() {}

/* SCAN
error: extend member 'foo' is not allowed to shadow members of 'extend A'
  ==> default_implement_19.cj:32:17:
   | 
32 |     public func foo(): Unit {
   |                 ^^^ 
   | 

error: extend member 'goo' is not allowed to shadow members of 'extend A'
  ==> default_implement_19.cj:35:24:
   | 
35 |     public static func goo(): Unit {
   |                        ^^^ 
   | 

error: extend member 'foo' is not allowed to shadow members of 'extend B'
  ==> default_implement_19.cj:45:17:
   | 
45 |     public func foo(): Unit {
   |                 ^^^ 
   | 

error: extend member 'goo' is not allowed to shadow members of 'extend B'
  ==> default_implement_19.cj:48:24:
   | 
48 |     public static func goo(): Unit {
   |                        ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
