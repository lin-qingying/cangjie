// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

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

func test<T>(a: T): Unit where T <: I0 & I1 {
    a.test
}

main() {}

/* SCAN
error: ambiguous use of 'test'
  ==> generic_upperbound_reference_06.cj:24:5:
   | 
24 |     a.test
   |     ^^^^^^ 
   | 
note: found function member of 'Interface-I0'
  ==> generic_upperbound_reference_06.cj:10:10:
   | 
10 |     func test() {}
   |          ^^^^ 
   | 
note: found property member of 'Interface-I1'
  ==> generic_upperbound_reference_06.cj:14:10:
   | 
14 |     prop test: Int64 {
   |          ^^^^ 
   | 

error: inherited members 'test' have inconsistent decl types
  ==> generic_upperbound_reference_06.cj:21:7:
   | 
21 | class A <: I0 & I1 {}
   |       ^ 
   | 
note: function member
  ==> generic_upperbound_reference_06.cj:10:10:
   | 
10 |     func test() {}
   |          ^^^^ 
   | 
note: property member
  ==> generic_upperbound_reference_06.cj:14:10:
   | 
14 |     prop test: Int64 {
   |          ^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
