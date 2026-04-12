// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo<T>() {
    1
}

func me<T>(a!: () -> Int64 = foo) {
    0
}

main(): Int64 {
    return me<Int64>() - 1
}

/* SCAN
error: generic type should be used with type argument
  ==> func_assign_var_02.cj:13:30:
   | 
13 | func me<T>(a!: () -> Int64 = foo) {
   |                              ^ 
   | 

1 error generated, 1 error printed.
*/
