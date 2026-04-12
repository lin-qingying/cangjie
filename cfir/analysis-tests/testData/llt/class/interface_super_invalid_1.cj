// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I1 {
    func a() {}
}

interface I2 <: I1 {
    func b() {
        super.a()
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: 'super' cannot be used in interface
  ==> interface_super_invalid_1.cj:15:9:
   | 
15 |         super.a()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
