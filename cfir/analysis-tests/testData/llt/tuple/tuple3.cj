// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a: Int64 = 1
    var b = a[0]
    return a
}

/* SCAN
error: invalid subscript operator [] on type 'Int64' with index type 'Int64'
  ==> tuple3.cj:11:13:
   | 
11 |     var b = a[0]
   |             ^^^^ 
   | 
   # note: you may want to implement 'operator func [](index0: Int64)' for type 'Int64'

1 error generated, 1 error printed.
*/
