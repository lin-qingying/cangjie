// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public var a: Int64
    public var b: Int64
    public init() {
        a = b
        b = 1
    }
}

/* SCAN
error: variable 'b' is used before initialization
  ==> class_init_constructor9.cj:13:13:
   | 
13 |         a = b
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
