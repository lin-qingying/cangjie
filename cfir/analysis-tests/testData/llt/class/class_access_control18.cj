// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface Data2 {
    static func foo(): Int64 {
        return 2
    }
}

interface Data3 <: Data2 {}

open class Test1 <: Data2 {
    private static func foo(a: Int64): Int64 {
        return 3
    }
    public func f(): Int64 {
        foo
        return 0
    }
}

class Test <: Test1 & Data2 & Data3 {}

main(): Int64 {
    return 1
}

/* SCAN
error: ambiguous use of 'foo'
  ==> class_access_control18.cj:22:9:
   | 
22 |         foo
   |         ^^^ 
   | 
note: found candidate
  ==> class_access_control18.cj:10:17:
   | 
10 |     static func foo(): Int64 {
   |                 ^^^ 
   | 
note: found candidate
  ==> class_access_control18.cj:18:25:
   | 
18 |     private static func foo(a: Int64): Int64 {
   |                         ^^^ 
   | 

1 error generated, 1 error printed.
*/
