// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public static var a: Int64 = 1
    public var b: Int64 = 2
    public static var c: Int64 = a + b
}

main(): Int64 {
    return 0
}

/* SCAN
error: 'b' is non-static member, cannot be accessed by static variable
  ==> class_static_member1.cj:12:5:
   | 
12 |     public static var c: Int64 = a + b
   |     ^ 
   | 

error: variable 'b' is used before initialization
  ==> class_static_member1.cj:12:38:
   | 
12 |     public static var c: Int64 = a + b
   |                                      ^ 
   | 

2 errors generated, 2 errors printed.
*/
