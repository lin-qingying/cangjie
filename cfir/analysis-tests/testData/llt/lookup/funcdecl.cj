// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

func a() {
    let a = b()
    let c = a1()

    func a1() {
    }
}

func b() {
    123
}

/* SCAN
error: undeclared identifier 'a1'
  ==> funcdecl.cj:11:13:
   | 
11 |     let c = a1()
   |             ^ 
   | 

error: 'main' is missing
 ==> funcdecl.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
