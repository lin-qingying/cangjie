// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    operator func [](index0: Int64, value!: Int64, invalid1!: Int64, invalid2!: Int64) {
        0
    }
}

main() {}

/* SCAN
error: overloaded operator '[]' can only have one named parameter 'value'
  ==> err_subscript_assign_05.cj:10:52:
   | 
10 |     operator func [](index0: Int64, value!: Int64, invalid1!: Int64, invalid2!: Int64) {
   |                                                    ^^^^^^^^          ~~~~~~~~ 
   | 

1 error generated, 1 error printed.
*/
