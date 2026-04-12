// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let t = while (true) {
    var l: Int32 = 0
    var r: Int64 = 1
    l + r
}

/* SCAN
error: invalid binary operator '+' on type 'Int32' and 'Int64'
  ==> binaryexpr.cj:12:7:
   | 
12 |     l + r
   |       ^ 
   | 
   # note: you may want to implement 'operator func +(right: Int64)' for type 'Int32'

1 error generated, 1 error printed.
*/
