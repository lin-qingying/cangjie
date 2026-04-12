// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public var a: Int64 = c
    public var b: Int64 = 1
}

let c: Int64 = 1

/* SCAN
error: variable 'c' is used before being defined
  ==> class_init_default_constructor1.cj:10:27:
   | 
10 |     public var a: Int64 = c
   |                           ^ 
   | 

1 error generated, 1 error printed.
*/
