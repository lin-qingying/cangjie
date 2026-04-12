// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I0 {
    func test() {}
}

interface I1 {
    prop test: Int64 {
        get() {
            1
        }
    }
}

class A <: I0 & I1 {}

class B {}

extend B <: I0 & I1 {}

main() {
    return 0
}

/* SCAN
error: inherited members 'test' have inconsistent decl types
  ==> interface_conflict_inheritance_02.cj:21:7:
   | 
21 | class A <: I0 & I1 {}
   |       ^ 
   | 
note: function member
  ==> interface_conflict_inheritance_02.cj:10:10:
   | 
10 |     func test() {}
   |          ^^^^ 
   | 
note: property member
  ==> interface_conflict_inheritance_02.cj:14:10:
   | 
14 |     prop test: Int64 {
   |          ^^^^ 
   | 

error: inherited members 'test' have inconsistent decl types
  ==> interface_conflict_inheritance_02.cj:25:1:
   | 
25 | extend B <: I0 & I1 {}
   | ^ 
   | 
note: function member
  ==> interface_conflict_inheritance_02.cj:10:10:
   | 
10 |     func test() {}
   |          ^^^^ 
   | 
note: property member
  ==> interface_conflict_inheritance_02.cj:14:10:
   | 
14 |     prop test: Int64 {
   |          ^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
