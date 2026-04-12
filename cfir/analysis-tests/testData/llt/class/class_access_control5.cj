// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface T1 {
    func f() {}
}

interface T2 <: T1 {
    func f() {}
}

open class D <: T2 {
    public open override func f() {}
}

class E <: D {
    protected override func f() {}
}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> class_access_control5.cj:22:29:
   | 
22 |     protected override func f() {}
   |                             ^ the visibility of the deriving 'f' is 'protected'
   | 
note: the visibility of the base 'f' is 'public'
  ==> class_access_control5.cj:18:31:
   | 
18 |     public open override func f() {}
   |                               ^ 
   | 

1 error generated, 1 error printed.
*/
