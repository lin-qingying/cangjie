// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface I1 {
    prop a: Int32 {
        get() {
            return 0
        }
    }
}

interface I2 {
    prop a: Int32 {
        get() {
            return 3
        }
    }
}

class C <: I1 & I2 {}

main() {
    return 0
}

/* SCAN
error: interface property 'a' must be implemented in 'C'
  ==> interface_property4.cj:25:1:
   | 
25 | class C <: I1 & I2 {}
   | ^ 
   | 

1 error generated, 1 error printed.
*/
