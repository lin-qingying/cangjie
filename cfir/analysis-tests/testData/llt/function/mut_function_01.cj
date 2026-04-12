// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S0 {
    var t0: Int64 = 0
    init() {}
}

struct S1 {
    var t1: S0 = S0()
    init() {}
}

struct S2 {
    var t2: S1 = S1()
    init() {}

    func f() {
        t2.t1.t0 += 10 // 非mut函数，不应该允许此类修改
    }
}

main(): Unit {
    let s2 = S2()
    s2.f()
    println(s2.t2.t1.t0.toString())
}

/* SCAN
error: instance member variable 't2' cannot be modified in immutable function
  ==> mut_function_01.cj:24:9:
   | 
24 |         t2.t1.t0 += 10 // 非mut函数，不应该允许此类修改
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
