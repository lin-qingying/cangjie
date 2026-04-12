// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public var a: Int32 = 1
    protected var d: Int32 = 2
}

class B <: A {
    private var d: Int32 = 1
}

main(): Int64 {
    return 0
}

/* SCAN
error: the variable 'd' must not shadow a member variable of the supertype
  ==> class_access_control8.cj:15:5:
   | 
15 |     private var d: Int32 = 1
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
