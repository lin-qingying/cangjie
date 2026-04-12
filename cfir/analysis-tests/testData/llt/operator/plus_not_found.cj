// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var a = 0.1 + 1
    return 0
}

/* SCAN
error: invalid binary operator '+' on type 'Float64' and 'Int64'
  ==> plus_not_found.cj:10:17:
   | 
10 |     var a = 0.1 + 1
   |                 ^ 
   | 
   # note: you may want to implement 'operator func +(right: Int64)' for type 'Float64'

1 error generated, 1 error printed.
*/
