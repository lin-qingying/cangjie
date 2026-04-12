// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct A {
    public let a: Int64
    public let b: Int64
    public let c: Int64
    public init(x: Int64, y: Int64) {
        this.a = x
        this.b = y
    }
    public init(x: Int64, y: Int64, z: Int64) {
        this.a = x
        this.b = y
        this.c = z
    }
}

main(): Int64 {
    0
}

/* SCAN
error: the uninitialized member variable 'c' is not initialized in the constructor of class or struct
  ==> record_constructor_call_other_init02.cj:13:5:
   | 
13 |     public init(x: Int64, y: Int64) {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
