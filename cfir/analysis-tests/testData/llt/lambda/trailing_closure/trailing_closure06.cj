// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

main(): Int64 {
    func f(a: Int64) {
        func g(h: (Int64) -> Int64) {
            h(0) - a
        }
        g
    }
    var t = f(1) {t => t + 1}
    return 0
}

/* SCAN
error: extra argument given for parameter list '(Int64)' in call
  ==> trailing_closure06.cj:16:14:
   | 
16 |     var t = f(1) {t => t + 1}
   |              ^^^^^^^^^^^^^^^^ expected 1 argument, found 2
   | 
note: found candidate
  ==> trailing_closure06.cj:10:10:
   | 
10 |     func f(a: Int64) {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
