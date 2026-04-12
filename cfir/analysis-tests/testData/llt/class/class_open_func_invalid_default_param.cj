// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {
    public open func test(a!: Int64 = 1) {
        0
    }
}

/* SCAN
error: optional parameter cannot be used in 'open' function
  ==> class_open_func_invalid_default_param.cj:10:27:
   | 
10 |     public open func test(a!: Int64 = 1) {
   |                           ^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
