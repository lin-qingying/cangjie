// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct A {}

extend A <: Any {}

class B {}

extend B <: Any {}

/* SCAN
error: interface 'Any' is not able to be extended
  ==> any_interface.cj:11:13:
   | 
11 | extend A <: Any {}
   |             ^^^ 
   | 

error: interface 'Any' is not able to be extended
  ==> any_interface.cj:15:13:
   | 
15 | extend B <: Any {}
   |             ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
