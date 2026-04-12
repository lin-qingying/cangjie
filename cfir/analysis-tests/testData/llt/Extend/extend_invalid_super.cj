// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class A {
    public let x: Int64 = 1
}

class B <: A {}

extend B {
    public func foo(): Int64 {
        return super.x
    }
}

/* SCAN
error: 'super' is not allowed inside an extend declaration
  ==> extend_invalid_super.cj:17:16:
   | 
17 |         return super.x
   |                ^^^^^ 
   | 

error: 'main' is missing
 ==> extend_invalid_super.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
