// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class C {}

class D {}

class E {}

interface I0 {
    func foo(): C
}

interface I1 {
    func foo(): D
}

interface I2 {
    func foo(): E
}

interface II <: I0 & I1 & I2 {}

interface III <: I1 & I2 {
    func foo(): E
}

main() {
    return 0
}

/* SCAN
error: return type of the inherited function members 'foo' are not identical and not in subtype relation
  ==> interface_conflict_inheritance_01.cj:27:11:
   | 
27 | interface II <: I0 & I1 & I2 {}
   |           ^^ 
   | 
   # note: conflict types are 'Class-C', 'Class-D', 'Class-E'

error: return type of 'foo' is not identical or not a subtype of the overridden/redefined/implement function
  ==> interface_conflict_inheritance_01.cj:30:10:
   | 
30 |     func foo(): E
   |          ^^^ 
   | 
note: 'Class-E' is not a subtype of 'Class-D'
  ==> interface_conflict_inheritance_01.cj:30:10:
   | 
30 |     func foo(): E
   |          ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
