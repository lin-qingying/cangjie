// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// struct cannot access member functions defined in its extend
enum E {
    A | B | C

    func foo() {
        go()
    }
}

extend E {
    func go() {}
}

/* SCAN
error: undeclared identifier 'go'
  ==> extend_namelookup9.cj:14:9:
   | 
14 |         go()
   |         ^ 
   | 

error: 'main' is missing
 ==> extend_namelookup9.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
