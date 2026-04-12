// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public var a = 1
}

open class B <: A {
    public var a = 10
}

class C <: B {
    public var a = 20
}

main() {
    return 1
}

/* SCAN
error: the variable 'a' must not shadow a member variable of the supertype
  ==> class_shadow_variable_2.cj:14:5:
   | 
14 |     public var a = 10
   |     ^ 
   | 

error: the variable 'a' must not shadow a member variable of the supertype
  ==> class_shadow_variable_2.cj:18:5:
   | 
18 |     public var a = 20
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
