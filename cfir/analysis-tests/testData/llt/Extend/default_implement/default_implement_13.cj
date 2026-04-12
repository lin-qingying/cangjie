// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {
    func foo() {}
}

interface I2 {
    func foo() {}
}

class A {}

extend A <: I1 {}

extend A <: I2 {}

main() {}

/* SCAN
error: interface function 'foo' must be implemented in 'extend A'
  ==> default_implement_13.cj:19:1:
   | 
19 | extend A <: I1 {}
   | ^ 
   | 

error: interface function 'foo' must be implemented in 'extend A'
  ==> default_implement_13.cj:21:1:
   | 
21 | extend A <: I2 {}
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
