// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    func foo() {}
}

open class C1 {}

extend C1 <: I1 {}

class D1 <: C1 {
    public func foo() {}
}

main() {}

/* SCAN
error: cannot override function 'foo' in extend of supertype
  ==> extend_override_1.cj:18:17:
   | 
18 |     public func foo() {}
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
