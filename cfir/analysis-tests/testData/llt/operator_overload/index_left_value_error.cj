// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    operator func [](arg1: Int64, value!: Int64): Unit {}
}

main() {
    var b = A()
    b[r'c'] = 3
    0
}

/* SCAN
error: cannot assign to this subscript expression
  ==> index_left_value_error.cj:15:5:
   | 
15 |     b[r'c'] = 3
   |     ^^^^^^^ 
   | 
   # note: you may want to implement 'operator func[](index0: Rune, value!: Int64)' for type 'Class-A'

1 error generated, 1 error printed.
*/
