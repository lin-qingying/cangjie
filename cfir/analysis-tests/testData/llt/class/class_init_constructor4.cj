// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {
    public var a: Int64
    public init(n: Int64) {
        var i: Int64 = 0
        while (i < n) {
            this.a = this.a + n
            i = i + 1
        }
    }
}

/* SCAN
error: variable 'a' is used before initialization
  ==> class_init_constructor4.cj:14:22:
   | 
14 |             this.a = this.a + n
   |                      ^ 
   | 

error: the uninitialized member variable 'a' is not initialized in the constructor of class or struct
  ==> class_init_constructor4.cj:11:5:
   | 
11 |     public init(n: Int64) {
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
