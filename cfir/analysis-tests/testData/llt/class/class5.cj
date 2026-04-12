// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class B {
    public var b = super.a
}

/* SCAN
error: 'a' is not a member of class 'Object'
  ==> class5.cj:10:26:
   | 
10 |     public var b = super.a
   |                          ^ 
   | 

error: 'super' is not allowed to initialize non-static member
  ==> class5.cj:10:20:
   | 
10 |     public var b = super.a
   |                    ^ 
   | 

2 errors generated, 2 errors printed.
*/
