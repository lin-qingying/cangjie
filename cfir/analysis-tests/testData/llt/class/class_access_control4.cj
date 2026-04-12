// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f %compile_lib_opt 2>&1 | compare %f

interface T {
    func f() {}
}

interface T1 <: T {
    func f() {}
}

abstract class B <: T {
    protected override func f() {}
}

open class C <: T {
    public open override func f() {}
}

public class D <: C {
    protected override func f() {}
}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> class_access_control4.cj:18:29:
   | 
18 |     protected override func f() {}
   |                             ^ the visibility of the deriving 'f' is 'protected'
   | 
note: the visibility of the base 'f' is 'public'
  ==> class_access_control4.cj:10:10:
   | 
10 |     func f() {}
   |          ^ the base 'f' is in 'interface'
   | 

error: a deriving member must be at least as visible as its base member
  ==> class_access_control4.cj:26:29:
   | 
26 |     protected override func f() {}
   |                             ^ the visibility of the deriving 'f' is 'protected'
   | 
note: the visibility of the base 'f' is 'public'
  ==> class_access_control4.cj:22:31:
   | 
22 |     public open override func f() {}
   |                               ^ 
   | 

2 errors generated, 2 errors printed.
*/
