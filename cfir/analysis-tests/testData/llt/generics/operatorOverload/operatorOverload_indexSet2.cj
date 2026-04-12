// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public operator func [](index: Int64, value!: Int64): Int64 {
        return 1;
    }
}

main(): Int64 {
    return 1;
}

/* SCAN
error: the return type of subscript assignment must be 'Unit'
  ==> operatorOverload_indexSet2.cj:10:59:
   | 
10 |     public operator func [](index: Int64, value!: Int64): Int64 {
   |                                                           ^^^^^ 
   | 

1 error generated, 1 error printed.
*/
