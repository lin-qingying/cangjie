// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface Data2 {}

interface Data3 <: Data2 {}

open class Test1 <: Data2 {
    public static func foo(): Int64 {
        return 3
    }
    private static func foo(a: Int64): Int64 {
        return 3
    }
    public func f(): Int64 {
        Test1.foo
        return 0
    }
}

class Test <: Test1 & Data2 & Data3 {}

main(): Int64 {
    Test1.foo
    return 0
}

/* SCAN
error: ambiguous use of 'foo'
  ==> class_access_control17.cj:21:9:
   | 
21 |         Test1.foo
   |         ^^^^^^^^^ 
   | 
note: found candidate
  ==> class_access_control17.cj:14:24:
   | 
14 |     public static func foo(): Int64 {
   |                        ^^^ 
   | 
note: found candidate
  ==> class_access_control17.cj:17:25:
   | 
17 |     private static func foo(a: Int64): Int64 {
   |                         ^^^ 
   | 

error: ambiguous use of 'foo'
  ==> class_access_control17.cj:29:5:
   | 
29 |     Test1.foo
   |     ^^^^^^^^^ 
   | 
note: found candidate
  ==> class_access_control17.cj:14:24:
   | 
14 |     public static func foo(): Int64 {
   |                        ^^^ 
   | 
note: found candidate
  ==> class_access_control17.cj:17:25:
   | 
17 |     private static func foo(a: Int64): Int64 {
   |                         ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
