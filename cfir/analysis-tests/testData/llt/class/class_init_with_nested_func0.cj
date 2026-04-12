// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public let a: Int32
    public init(b: Int64) {
        func f(): Int32 {
            a = 8
            return 1
        }
        f()
    }
}

/* SCAN
error: cannot capture variable 'a' before initialization
  ==> class_init_with_nested_func0.cj:13:13:
   | 
13 |             a = 8
   |             ^ 
   | 

error: the uninitialized member variable 'a' is not initialized in the constructor of class or struct
  ==> class_init_with_nested_func0.cj:11:5:
   | 
11 |     public init(b: Int64) {
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
