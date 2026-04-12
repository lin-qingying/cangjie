// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f %compile_lib_opt 2>&1 | compare %f

open class A {
    public open func abc() {}
}

class B <: A {
    protected override func abc() {}
}

open class D {
    public open func f() {}
}

public open class E <: D {
    public open override func f() {}
}

class F <: E {
    private override func f() {}
}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> class_access_control3.cj:14:29:
   | 
14 |     protected override func abc() {}
   |                             ^^^ the visibility of the deriving 'abc' is 'protected'
   | 
note: the visibility of the base 'abc' is 'public'
  ==> class_access_control3.cj:10:22:
   | 
10 |     public open func abc() {}
   |                      ^^^ 
   | 

error: a deriving member must be at least as visible as its base member
  ==> class_access_control3.cj:26:27:
   | 
26 |     private override func f() {}
   |                           ^ the visibility of the deriving 'f' is 'private'
   | 
note: the visibility of the base 'f' is 'public'
  ==> class_access_control3.cj:22:31:
   | 
22 |     public open override func f() {}
   |                               ^ 
   | 

2 errors generated, 2 errors printed.
*/
