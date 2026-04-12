// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public var a: Int64 = 1
}

class B <: A {
    public var b: Int64 = 2
    public func f() {
        var c: Int32 = a
        var d: Int32 = b
    }
}

class C <: D {} // regex inheritance cycle detected: 'D->C->D'

class D <: C {}

/* SCAN
error: inheritance cycle detected: 'C->D->C'
  ==> class1.cj:21:1:
   | 
21 | class C <: D {} // regex inheritance cycle detected: 'D->C->D'
   | ^ 
   | 

error: super class 'A' is not inheritable
  ==> class1.cj:13:12:
   | 
13 | class B <: A {
   |            ^ 
   | 

error: super class 'D' is not inheritable
  ==> class1.cj:21:12:
   | 
21 | class C <: D {} // regex inheritance cycle detected: 'D->C->D'
   |            ^ 
   | 

error: super class 'C' is not inheritable
  ==> class1.cj:23:12:
   | 
23 | class D <: C {}
   |            ^ 
   | 

error: mismatched types
  ==> class1.cj:16:24:
   | 
16 |         var c: Int32 = a
   |                        ^ expected 'Int32', found 'Int64'
   | 

error: mismatched types
  ==> class1.cj:17:24:
   | 
17 |         var d: Int32 = b
   |                        ^ expected 'Int32', found 'Int64'
   | 

error: recursive constructor calling detected
  ==> class1.cj:23:7:
   | 
23 | class D <: C {}
   |       ^ 
   | 
note: depends on
  ==> class1.cj:21:7:
   | 
21 | class C <: D {} // regex inheritance cycle detected: 'D->C->D'
   |       ^ 
   | 
note: depends on
  ==> class1.cj:23:7:
   | 
23 | class D <: C {}
   |       ^ 
   | 

7 errors generated, 7 errors printed.
*/
