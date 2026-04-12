// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

struct Test {
    private Test(var name: String) {}
    init(name: String) {}
}

/* SCAN
error: constructor 'init' has overload conflicts
  ==> primary_ctor.cj:11:5:
   | 
11 |     init(name: String) {}
   |     ^^^^ 
   | 
note: conflict with the declaration
  ==> primary_ctor.cj:10:13:
   | 
10 |     private Test(var name: String) {}
   |             ^^^^ 
   | 

error: the uninitialized member variable 'name' is not initialized in the constructor of class or struct
  ==> primary_ctor.cj:11:5:
   | 
11 |     init(name: String) {}
   |     ^ 
   | 

error: 'main' is missing
 ==> primary_ctor.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
