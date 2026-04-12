// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    S(var x!: Int64) {}

    mut operator func [](i: Int64, value!: Int64) {}
}

main() {
    var s: ?S = S(x: 3)
    s?[0] = 9
    return
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> err_assign_00.cj:17:6:
   | 
17 |     s?[0] = 9
   |      ^ is immutable
   | 

1 error generated, 1 error printed.
*/
