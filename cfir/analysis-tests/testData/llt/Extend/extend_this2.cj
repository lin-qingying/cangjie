// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    public let x: Int64 = 1
}

extend A {
    public func foo(): Int64 {
        return this.y
    }
}

/* SCAN
error: 'y' is not a member of class 'A'
  ==> extend_this2.cj:15:21:
   | 
15 |         return this.y
   |                     ^ 
   | 

error: 'main' is missing
 ==> extend_this2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
