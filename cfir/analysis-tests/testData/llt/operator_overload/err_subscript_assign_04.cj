// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    operator func [](index0: Int64, value!: Int64) {
        0
    }
}

main() {}

/* SCAN
error: the return type of subscript assignment must be 'Unit'
  ==> err_subscript_assign_04.cj:10:19:
   | 
10 |     operator func [](index0: Int64, value!: Int64) {
   |                   ^^ 
   | 

1 error generated, 1 error printed.
*/
