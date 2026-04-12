// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I2 {
    func foo(): Unit
}

open class C2 {}

extend C2 <: I2 {
    public func foo() {}
}

class D2 <: C2 {
    public func foo() {}
}

main() {}

/* SCAN
error: cannot override function 'foo' in extend of supertype
  ==> extend_override_2.cj:20:17:
   | 
20 |     public func foo() {}
   |                 ^^^ 
   | 
note: overridden definition of 'foo' is here
  ==> extend_override_2.cj:16:17:
   | 
16 |     public func foo() {}
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
