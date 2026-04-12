// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    static init() {
        if (1 > 2) {
            return
        } else {
            return
        }
    }
}

main() {
    return 0
}

/* SCAN
error: 'return' cannot be used inside the static initializer
  ==> static_init_04.cj:14:13:
   | 
14 |             return
   |             ^^^^^^ 
   | 

error: 'return' cannot be used inside the static initializer
  ==> static_init_04.cj:12:13:
   | 
12 |             return
   |             ^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
