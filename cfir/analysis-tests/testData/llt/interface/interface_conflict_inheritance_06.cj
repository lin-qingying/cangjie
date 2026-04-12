// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I1 {
    func f(): Bool {
        true
    }
}

interface I2 {
    func f(): Rune
}

class E <: Exception {}

class A <: I1 & I2 {}

main() {
}

/* SCAN
error: return type of the inherited function members 'f' are not identical and not in subtype relation
  ==> interface_conflict_inheritance_06.cj:21:7:
   | 
21 | class A <: I1 & I2 {}
   |       ^ 
   | 
   # note: conflict types are 'Bool', 'Rune'

1 error generated, 1 error printed.
*/
