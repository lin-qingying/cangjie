// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    operator func [](index: Int64, value: Int64) {}
}

main() {
    var a = A()
    a[1] = 4
}

/* SCAN
error: cannot assign to this subscript expression
  ==> err_subscript_assign_00.cj:15:5:
   | 
15 |     a[1] = 4
   |     ^^^^ 
   | 
   # note: you may want to implement 'operator func[](index0: Int64, value!: Int64)' for type 'Class-A'

1 error generated, 1 error printed.
*/
