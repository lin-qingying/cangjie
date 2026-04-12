// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {}

extend A {
    public func foo() {}
}

extend A {
    public func foo() {}
}

/* SCAN
error: extend member 'foo' is not allowed to shadow members of 'extend A'
  ==> extend_member_conflict1.cj:12:17:
   | 
12 |     public func foo() {}
   |                 ^^^ 
   | 

error: extend member 'foo' is not allowed to shadow members of 'extend A'
  ==> extend_member_conflict1.cj:16:17:
   | 
16 |     public func foo() {}
   |                 ^^^ 
   | 

error: 'main' is missing
 ==> extend_member_conflict1.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
