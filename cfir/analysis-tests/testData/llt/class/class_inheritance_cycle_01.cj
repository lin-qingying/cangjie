// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A <: AA {}

type AA = A

/* SCAN
error: inheritance cycle detected: 'A->A'
 ==> class_inheritance_cycle_01.cj:9:1:
  | 
9 | open class A <: AA {}
  | ^ 
  | 

error: recursive constructor calling detected
 ==> class_inheritance_cycle_01.cj:9:12:
  | 
9 | open class A <: AA {}
  |            ^ 
  | 
note: depends on
 ==> class_inheritance_cycle_01.cj:9:12:
  | 
9 | open class A <: AA {}
  |            ^ 
  | 

2 errors generated, 2 errors printed.
*/
