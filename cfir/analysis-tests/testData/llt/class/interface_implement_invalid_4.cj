// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    func foo(): Int64
}

interface I2 <: I1 {
    func foo(): Int64 {
        return 2
    }
}

interface I3 <: I1 {
    func foo(): Int64 {
        return 3
    }
}

class C1 <: I2 & I3 {}

main(): Int64 {
    return 0
}

/* SCAN
error: interface function 'foo' must be implemented in 'C1'
  ==> interface_implement_invalid_4.cj:25:1:
   | 
25 | class C1 <: I2 & I3 {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
