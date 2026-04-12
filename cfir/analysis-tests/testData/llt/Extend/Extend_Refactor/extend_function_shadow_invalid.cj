// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I1 {
    func foo(): Int64 {
        1
    }
}

interface I2 {
    func foo(): Int64 {
        2
    }
}

class C1 {}

extend C1 {
    public func foo(): Int64 {
        3
    }
}

extend C1 <: I1 {}

extend C1 <: I2 {}

main() {
    return 0
}

/* SCAN
error: extend member 'foo' is not allowed to shadow members of 'extend C1'
  ==> extend_function_shadow_invalid.cj:24:17:
   | 
24 |     public func foo(): Int64 {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
