// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class B {}

class A <: B {
    public var a = super()
}

main() {
    A()
    C()
    0
}

class C {
    public var a = super()
}

/* SCAN
error: invalid calling 'super' outside the constructor
  ==> invalid_super_call_2.cj:12:20:
   | 
12 |     public var a = super()
   |                    ^ 
   | 

error: invalid calling 'super' outside the constructor
  ==> invalid_super_call_2.cj:22:20:
   | 
22 |     public var a = super()
   |                    ^ 
   | 

2 errors generated, 2 errors printed.
*/
