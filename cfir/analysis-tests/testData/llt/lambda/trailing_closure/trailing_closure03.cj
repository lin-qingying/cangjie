// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

main(): Int64 {
    func f() {
        func g(h: (Int64) -> Int64) {
            h(0)
        }
        g
    }
    f() {t => t + 1}
    return 0
}

/* SCAN
error: extra argument given for parameter list '()' in call
  ==> trailing_closure03.cj:16:6:
   | 
16 |     f() {t => t + 1}
   |      ^^^^^^^^^^^^^^^ expected 0 arguments, found 1
   | 
note: found candidate
  ==> trailing_closure03.cj:10:10:
   | 
10 |     func f() {
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
