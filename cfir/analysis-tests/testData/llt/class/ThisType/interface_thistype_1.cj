// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I1 {
    func foo(a: This): This 
}

main() {
    return 0
}

/* SCAN
error: 'This' type is not allowed
  ==> interface_thistype_1.cj:10:17:
   | 
10 |     func foo(a: This): This 
   |                 ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

error: 'This' type is not allowed
  ==> interface_thistype_1.cj:10:24:
   | 
10 |     func foo(a: This): This 
   |                        ^^^^ 
   | 
   # note: 'This' type can only be used as the return type of an instance member function in class

2 errors generated, 2 errors printed.
*/
