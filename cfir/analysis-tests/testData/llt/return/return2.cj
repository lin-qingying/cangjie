// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

class A {
let m2 = return 
}

struct B {
let m2 :Unit = return 
}

interface C {
let m2 = return 
}

var a = return

/* SCAN
error: unexpected variable declaration in interface body
  ==> return2.cj:18:1:
   | 
17 | interface C {
   | ~~~~~~~~~ in interface
18 | let m2 = return 
   | ^^^ unexpected variable declaration
   | 

1 error generated, 1 error printed.
*/
