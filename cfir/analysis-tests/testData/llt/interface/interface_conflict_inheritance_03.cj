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

interface I2 <: I0 {}

class A <: I2 & I1 {}

main() {
    return 0
}

/* SCAN
error: inherited members 'test' have inconsistent decl types
  ==> interface_conflict_inheritance_03.cj:23:7:
   | 
23 | class A <: I2 & I1 {}
   |       ^ 
   | 
note: function member
  ==> interface_conflict_inheritance_03.cj:10:10:
   | 
10 |     func test() {}
   |          ^^^^ 
   | 
note: property member
  ==> interface_conflict_inheritance_03.cj:14:10:
   | 
14 |     prop test: Int64 {
   |          ^^^^ 
   | 

1 error generated, 1 error printed.
*/
