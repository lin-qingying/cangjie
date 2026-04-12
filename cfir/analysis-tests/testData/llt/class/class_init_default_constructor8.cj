// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public static var a: Int64
}

/* SCAN
error: the static member variable 'a' is not initialized
  ==> class_init_default_constructor8.cj:10:5:
   | 
10 |     public static var a: Int64
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may add an initializer inline or define a 'static init' to perform initialization

1 error generated, 1 error printed.
*/
