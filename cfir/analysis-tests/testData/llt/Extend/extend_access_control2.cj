// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// accessibility: functions in the extend body cannot access private members inside the class body
class A {
    private func goo() {}
}

extend A {
    func foo() {
        goo()
        0
    }
}

/* SCAN
error: no matching function declaration for function call 'goo'
  ==> extend_access_control2.cj:16:9:
   | 
16 |         goo()
   |         ^ 
   | 

error: 'main' is missing
 ==> extend_access_control2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
