// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo() {
        i = i + 1
    }
}

main() {
    let obj = R1()
    obj.foo()
    return 0
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> record_extend_mut_invalid_5.cj:21:5:
   | 
21 |     obj.foo()
   |     ^^^ ~~~ is a mutable function
   |     |
   |     is immutable
   | 
note: 'obj' is a variable declared with 'let'
  ==> record_extend_mut_invalid_5.cj:20:9:
   | 
20 |     let obj = R1()
   |         ^^^ 
   | 

1 error generated, 1 error printed.
*/
