// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class C <: A {
    public init() {
        a = 0
        b = 0
    }
    public init(c: Int32) {
        a = 0
        b = 0
    }
}

open class A {
    public var a: Int32 = 1
    public let b: Int32
}

main(): Int64 {
    return 0
}

/* SCAN
error: the uninitialized member variable 'b' is not initialized in the constructor of class or struct
  ==> let_in_init12-1.cj:22:5:
   | 
22 |     public let b: Int32
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
