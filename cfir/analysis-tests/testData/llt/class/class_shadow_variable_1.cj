// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public var a = 1
}

class B <: A {
    public var a = 10
}

main() {
    return 1
}

/* SCAN
error: the variable 'a' must not shadow a member variable of the supertype
  ==> class_shadow_variable_1.cj:14:5:
   | 
14 |     public var a = 10
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
