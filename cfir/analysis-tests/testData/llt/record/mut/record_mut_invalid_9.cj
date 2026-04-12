// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1<T> {
    public var i: T

    public init(x: T) {
        i = x
    }

    public mut func foo(x: T) {
        i = x
    }
}

main() {
    let obj = R1<Int64>(0)
    obj.foo(1)
    return 1
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> record_mut_invalid_9.cj:23:5:
   | 
23 |     obj.foo(1)
   |     ^^^ ~~~ is a mutable function
   |     |
   |     is immutable
   | 
note: 'obj' is a variable declared with 'let'
  ==> record_mut_invalid_9.cj:22:9:
   | 
22 |     let obj = R1<Int64>(0)
   |         ^^^ 
   | 

1 error generated, 1 error printed.
*/
