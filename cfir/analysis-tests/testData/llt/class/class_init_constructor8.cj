// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {
    public var a: Int64
    public init() {
        if (a > 0) {
            a = 8
        } else {
            a = 4
        }
    }
}

/* SCAN
error: variable 'a' is used before initialization
  ==> class_init_constructor8.cj:12:13:
   | 
12 |         if (a > 0) {
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
