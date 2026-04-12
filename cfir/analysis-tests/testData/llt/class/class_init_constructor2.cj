// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {
    public var a: Int64
    public var b: Int64
    public init(n: Int64) {
        init(n, n)
        a = n
    }
    public init(n: Int64, m: Int64) {
        a = n
        b = m
    }
}

class B <: A {
    public var c: Int64
    public init() {
        c = 0
    }
}

/* SCAN
error: expected expression or declaration, found keyword 'init'
  ==> class_init_constructor2.cj:13:9:
   | 
13 |         init(n, n)
   |         ^^^^ expected expression or declaration here
   | 

1 error generated, 1 error printed.
*/
