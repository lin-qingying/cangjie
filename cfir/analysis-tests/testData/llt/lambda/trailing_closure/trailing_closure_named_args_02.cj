// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

main(): Int64 {
    func g(h!: (Int64) -> Int64, a!: (Int64) -> Int64) {
        h(0) - a(2)
    }
    var t = g(a: {t => t + 1}) {t => t + 1}
    return 0
}

/* SCAN
error: named argument prefix 'a:' cannot appeared more than once in call
  ==> trailing_closure_named_args_02.cj:13:32:
   | 
13 |     var t = g(a: {t => t + 1}) {t => t + 1}
   |                                ^ 
   | 

1 error generated, 1 error printed.
*/
