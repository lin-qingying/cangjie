// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class A<T> {
    let x: T
    init(x: T) {
        this.x = x
    }
}

class B <: A<Int64> {
    let y = x
    init(x: Int64) {
        super(x)
    }
}

main() {
    println(B(10).y)
}

/* SCAN
error: super member 'x' is not allowed to be used before calling super()
  ==> variable_use_before_init_13.cj:17:13:
   | 
17 |     let y = x
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
