// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// Test for side effect of compound calculation with property
var count = 0

class A {
    var a_ = 0
    mut prop a: Int64 {
        get() {
            a_
        }
        set(v) {
            a_ = v
        }
    }
    init() {
        count++
    }
}

main() {
    A().a += 1
    if (count != 1) {
        return 1
    }
    A().a++
    if (count != 2) {
        return 2
    }
    return 0
}
