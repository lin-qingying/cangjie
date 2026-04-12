// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

struct A {
    public var b: Int64 = 1
    public let c: B = B()
}

class B {
    public let c: A = A()
    public var d: Int64 = 1
}

func m(b: A) {
    b.b = 1
}

class FooArray {
    public init() {}
    public operator func [](it: FooArray, index: Int64): A {
        return A()
    }
}

type F = A

main() {
    let a = B()
    a.c.b = 1
    let b = A()
    b.b = 1
    b.c.d = 3 // OK
    let arr = FooArray()
    arr[0].b = 4
    let f: F = A()
    f.b = 1
    return 0
}

/* SCAN
error: recursive constructor calling detected
  ==> record_vardecl_check.cj:15:23:
   | 
15 |     public let c: A = A()
   |                       ^ 
   | 
note: depends on
 ==> record_vardecl_check.cj:9:8:
  | 
9 | struct A {
  |        ^ 
  | 
note: depends on
  ==> record_vardecl_check.cj:11:23:
   | 
11 |     public let c: B = B()
   |                       ^ 
   | 
note: depends on
  ==> record_vardecl_check.cj:14:7:
   | 
14 | class B {
   |       ^ 
   | 
note: depends on
  ==> record_vardecl_check.cj:15:23:
   | 
15 |     public let c: A = A()
   |                       ^ 
   | 

error: invalid subscript operator [] on type 'Class-FooArray' with index type 'Int64'
  ==> record_vardecl_check.cj:39:5:
   | 
39 |     arr[0].b = 4
   |     ^^^^^^ 
   | 
   # note: you may want to implement 'operator func [](index0: Int64)' for type 'Class-FooArray'

error: cannot assign to immutable value
  ==> record_vardecl_check.cj:20:5:
   | 
20 |     b.b = 1
   |     ^^^^^^^ 
   | 
note: parameter 'b' is immutable
  ==> record_vardecl_check.cj:19:8:
   | 
19 | func m(b: A) {
   |        ^ 
   | 

error: cannot assign to immutable value
  ==> record_vardecl_check.cj:34:5:
   | 
34 |     a.c.b = 1
   |     ^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> record_vardecl_check.cj:10:16:
   | 
10 |     public var b: Int64 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> record_vardecl_check.cj:36:5:
   | 
36 |     b.b = 1
   |     ^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> record_vardecl_check.cj:35:9:
   | 
35 |     let b = A()
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> record_vardecl_check.cj:41:5:
   | 
41 |     f.b = 1
   |     ^^^^^^^ 
   | 
note: variable 'f' is immutable
  ==> record_vardecl_check.cj:40:9:
   | 
40 |     let f: F = A()
   |         ^ 
   | 

6 errors generated, 6 errors printed.
*/
