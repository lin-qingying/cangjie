// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f 2>&1 | compare %f

interface A {
    func i() {}
    static func test() {
        i()
    }
}

/* SCAN
error: 'i' is non-static member, cannot be accessed by static function 'test'
  ==> class11_test1.cj:12:9:
   | 
12 |         i()
   |         ^ 
   | 

error: 'main' is missing
 ==> class11_test1.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
