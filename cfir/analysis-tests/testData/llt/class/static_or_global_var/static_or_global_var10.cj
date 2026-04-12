// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class A {
    public static var ctx: Int64

    static init() {
        let l1 = {=> println(ctx)}
        let l2 = {=> ctx = 10}
        l2()
        l1()
    }
}

main() {}

// ASSERT: scan the static member variable 'ctx' is not initialized