// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend B {
    public func foo() {
        return this;
    }
}

extend A {
    public func foo() {
        return this;
    }
}

/* SCAN
error: undeclared type name 'B'
 ==> extend_invalid_type.cj:9:8:
  | 
9 | extend B {
  |        ^ 
  | 

error: undeclared type name 'A'
  ==> extend_invalid_type.cj:15:8:
   | 
15 | extend A {
   |        ^ 
   | 

error: 'main' is missing
 ==> extend_invalid_type.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
