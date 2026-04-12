// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    operator func [](valu!: Int64) {}
}

main() {}

/* SCAN
error: overloaded operator '[]' can only have one named parameter 'value'
  ==> err_subscript_assign_02.cj:10:22:
   | 
10 |     operator func [](valu!: Int64) {}
   |                      ^^^^ 
   | 

error: overloaded operator '[]' should have at least one positional parameter for index
  ==> err_subscript_assign_02.cj:10:19:
   | 
10 |     operator func [](valu!: Int64) {}
   |                   ^^ 
   | 

2 errors generated, 2 errors printed.
*/
