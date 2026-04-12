// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

class Input {}

main() {
    let n: Int64 = 1
    let cond = false
    var input = Input()
    input[0] = 1
}

/* SCAN
error: cannot assign to this subscript expression
  ==> index_assign_00.cj:15:5:
   | 
15 |     input[0] = 1
   |     ^^^^^^^^ 
   | 
   # note: you may want to implement 'operator func[](index0: Int64, value!: Int64)' for type 'Class-Input'

1 error generated, 1 error printed.
*/
