// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

interface Data2 {
    func foo(a: Int64): Int64 {
        return 2
    }
}

interface Data3 <: Data2 {}

open class Test1 <: Data2 {
    public func foo(): Int64 {
        return 3
    }
}

class Test <: Test1 & Data2 & Data3 {}

main(): Int64 {
    var a = Test().foo
    return 0
}

/* SCAN
error: ambiguous use of 'foo'
  ==> class_access_control16.cj:26:13:
   | 
26 |     var a = Test().foo
   |             ^^^^^^^^^^ 
   | 
note: found candidate
  ==> class_access_control16.cj:10:10:
   | 
10 |     func foo(a: Int64): Int64 {
   |          ^^^ 
   | 
note: found candidate
  ==> class_access_control16.cj:18:17:
   | 
18 |     public func foo(): Int64 {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
