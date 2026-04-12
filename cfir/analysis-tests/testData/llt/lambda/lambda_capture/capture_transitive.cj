// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a = 1
    func f() {
        return a
    }
    func g() {
        return f() + 1
    }
    {a: Int64 => g}
    return 0
}

/* SCAN
error: g captured a mutable variable transitively, g cannot be used as a expression
  ==> capture_transitive.cj:17:18:
   | 
17 |     {a: Int64 => g}
   |                  ^ 
   | 

1 error generated, 1 error printed.
*/
