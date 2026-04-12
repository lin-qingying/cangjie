// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    var `init` = 1
    prop `init`: Int64 {
        get() {
            1
        }
    }
}

main() {
}

/* SCAN
error: redefinition of declaration 'init'
  ==> raw_init_main03.cj:11:11:
   | 
11 |     prop `init`: Int64 {
   |           ^^^^ 
   | 
note: 'init' is previously declared here
  ==> raw_init_main03.cj:10:10:
   | 
10 |     var `init` = 1
   |          ^^^^ 
   | 

1 error generated, 1 error printed.
*/
