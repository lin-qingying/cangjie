// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class C {
    static init() {}
    static init() {
        1
    }
}

main() {
    return 0
}

/* SCAN
error: static constructor 'init' has overload conflicts
  ==> static_init_02.cj:11:12:
   | 
11 |     static init() {
   |            ^^^^ 
   | 
note: conflict with the declaration
  ==> static_init_02.cj:10:12:
   | 
10 |     static init() {}
   |            ^^^^ 
   | 

1 error generated, 1 error printed.
*/
