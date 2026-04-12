// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    func foo(): Int64 {
        1
    }
}

interface I2 {
    func foo(): Int32 {
        2
    }
}

class C1 {}

extend C1 <: I1 & I2 {
    public func foo(): Int32 {
        2
    }
}

main() {
    return -1
}

/* SCAN
error: return type of 'foo' is not identical or not a subtype of the overridden/redefined/implement function
  ==> extend_function_conflict_invalid_6.cj:24:17:
   | 
24 |     public func foo(): Int32 {
   |                 ^^^ 
   | 
note: 'Int32' is not a subtype of 'Int64'
  ==> extend_function_conflict_invalid_6.cj:24:17:
   | 
24 |     public func foo(): Int32 {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
