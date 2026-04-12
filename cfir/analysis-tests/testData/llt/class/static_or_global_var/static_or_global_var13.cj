// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC-PIPE-0: %run %run_opt %output |compare %f

// ASSERT: scan 1

class A {
    public static var ctx: Int64 = 1

    static init() {
        let l2 = {=> ctx = 10}
    }
}

class B {
    public static var ctx : Int64

    static init() {
        ctx = 10
        let l1 = {=> println(ctx)}
        let l2 = {=> ctx = 10}
        l1()
        l2()
    }
}

main() {}
