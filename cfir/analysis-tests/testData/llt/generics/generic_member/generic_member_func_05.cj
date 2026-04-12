// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

open class A<X> {
    public static func test<T>(x: X): X {
        println("a");
        x
    }
    public func goo<T>(x: T): T {
        println("a");
        x
    }
}

class B<X> <: A<X> {
    public static redef func test<T>(x: T): T {
        println("b");
        x
    }
    public func goo<Y>(x: Y): Y {
        println("b");
        x
    }
}

main() {
    0
}

/* SCAN
error: cannot override function 'goo'
  ==> generic_member_func_05.cj:25:17:
   | 
25 |     public func goo<Y>(x: Y): Y {
   |                 ^^^ 
   | 
note: the parent function is not modified by 'open'
  ==> generic_member_func_05.cj:14:17:
   | 
14 |     public func goo<T>(x: T): T {
   |                 ^^^ 
   | 

error: 'redef' function 'test' does not have a redefined 'static' function in its supertype
  ==> generic_member_func_05.cj:21:5:
   | 
21 |     public static redef func test<T>(x: T): T {
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
