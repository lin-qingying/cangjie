// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {
    func f1(a!: Int64): Unit
    static func f2(a!: Int64 = 1, b!: Int64): Unit
}

/* SCAN
error: optional parameter cannot be used in abstract function
  ==> abstract_function1.cj:11:20:
   | 
11 |     static func f2(a!: Int64 = 1, b!: Int64): Unit
   |                    ^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
