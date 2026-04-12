// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {
    public var a: Int64
    public init(n: Int64) {
        a = n
    }
}

class B <: A {}

/* SCAN
error: there is no non-parameter constructor in super class, please invoke super call explicitly
  ==> class_init_default_constructor4.cj:16:7:
   | 
16 | class B <: A {}
   |       ^ 
   | 

1 error generated, 1 error printed.
*/
