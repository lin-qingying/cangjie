// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    operator func [](index: Int64, valu!: Int64) {}
}

main() {}

/* SCAN
error: overloaded operator '[]' can only have one named parameter 'value'
  ==> err_subscript_assign_01.cj:10:36:
   | 
10 |     operator func [](index: Int64, valu!: Int64) {}
   |                                    ^^^^ 
   | 

1 error generated, 1 error printed.
*/
