// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class C1 {}

extend C1 {
    public func foo(): Int64 {
        0
    }
}

class C2 <: C1 {
    public func foo(): Int64 {
        0
    }
}

main() {
    return -1
}

/* SCAN
error: cannot override function 'foo' in extend of supertype
  ==> extend_function_override_invalid_1.cj:18:17:
   | 
18 |     public func foo(): Int64 {
   |                 ^^^ 
   | 
note: overridden definition of 'foo' is here
  ==> extend_function_override_invalid_1.cj:12:17:
   | 
12 |     public func foo(): Int64 {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
