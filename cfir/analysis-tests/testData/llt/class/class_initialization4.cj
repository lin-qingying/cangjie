// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public static var a = A.b
    public static var b = 1
}

/* SCAN
error: variable 'b' is used before initialization
  ==> class_initialization4.cj:10:27:
   | 
10 |     public static var a = A.b
   |                           ^ 
   | 

error: global/static variable 'b' is used before initialization during initializing 'a'
  ==> class_initialization4.cj:10:29:
   | 
10 |     public static var a = A.b
   |                             ^ 
   | 

2 errors generated, 2 errors printed.
*/
