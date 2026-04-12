// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I0 {
    func f() {
        this()
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: invalid calling 'this' outside the constructor
  ==> interface_init_invalid_1.cj:11:9:
   | 
11 |         this()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
