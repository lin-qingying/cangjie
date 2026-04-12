// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public var a: Int32 = 1
    public let d: Int32 = 2
}

class B <: A {
    private var a: Int32 = 2
    protected let d: Int32 = 3
}

main(): Int64 {
    return 0
}

/* SCAN
error: the variable 'a' must not shadow a member variable of the supertype
  ==> class_access_control9.cj:15:5:
   | 
15 |     private var a: Int32 = 2
   |     ^ 
   | 

error: the variable 'd' must not shadow a member variable of the supertype
  ==> class_access_control9.cj:16:5:
   | 
16 |     protected let d: Int32 = 3
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
