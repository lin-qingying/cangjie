// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I1 {
    mut static prop a: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    static func f(): Unit {}
}

interface I2 <: I1 {
    mut static prop a: Int64
    static func f(): Unit
}

main() {}

/* SCAN
error: cannot override implemented interface property 'a' with abstract property
  ==> interface_conflict_inheritance_05.cj:20:5:
   | 
20 |     mut static prop a: Int64
   |     ^ 
   | 

error: cannot override implemented interface function 'f' with abstract function
  ==> interface_conflict_inheritance_05.cj:21:5:
   | 
21 |     static func f(): Unit
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
