// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    var b: Int64 = 0
}

main() {
    var a: ?A = A()
    a?.b = 9
    return
}

/* SCAN
error: cannot assign to immutable value
  ==> err_assign_01.cj:15:5:
   | 
15 |     a?.b = 9
   |     ^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
