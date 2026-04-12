// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

interface A <: B {}

interface C <: A {}

interface B <: C {}

extend A <: C {}

main() {
    0
}

/* SCAN
error: extending type 'Interface-A' is not allowed
  ==> extend_inheritance_circle3.cj:15:8:
   | 
15 | extend A <: C {}
   |        ^ 
   | 

error: inheritance cycle detected: 'A->B->C->A'
 ==> extend_inheritance_circle3.cj:9:1:
  | 
9 | interface A <: B {}
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
