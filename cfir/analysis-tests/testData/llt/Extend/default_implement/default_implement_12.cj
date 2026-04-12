// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {
    func foo() {}
}

interface I2 <: I1 {}

class A {}

extend A <: I1 & I2 {} // failed

main() {
    A().foo()
}

/* SCAN
error: interface function 'foo' must be implemented in 'extend A'
  ==> default_implement_12.cj:17:1:
   | 
17 | extend A <: I1 & I2 {} // failed
   | ^ 
   | 

1 error generated, 1 error printed.
*/
