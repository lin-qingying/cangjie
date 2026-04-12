// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {}

open class B {}

class C <: A & B {}

interface I {}

class D <: I & A {}

/* SCAN
error: only one super class may appear in supertype list of class 'C'
  ==> syntax.cj:13:16:
   | 
13 | class C <: A & B {}
   |                ^ 
   | 

error: super class 'A' must be placed at the beginning of supertype list
  ==> syntax.cj:17:16:
   | 
17 | class D <: I & A {}
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
