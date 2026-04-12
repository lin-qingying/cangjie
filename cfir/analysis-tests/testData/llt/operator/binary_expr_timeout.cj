// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var a = "aaa"
    var b = 3 + 4 + 5 + a + 5 + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 +
        a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a
    0
}

/* SCAN
error: invalid binary operator '+' on type 'Int64' and 'Struct-String'
  ==> binary_expr_timeout.cj:11:23:
   | 
11 |     var b = 3 + 4 + 5 + a + 5 + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 + a + 5 + a + 3 + a + a + 5 +
   |                       ^ 
   | 
   # note: you may want to implement 'operator func +(right: Struct-String)' for type 'Int64'

1 error generated, 1 error printed.
*/
