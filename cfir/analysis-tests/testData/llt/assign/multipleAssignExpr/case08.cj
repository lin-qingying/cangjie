// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    operator func [](index: Int64): A {
        A()
    }
    operator func [](index: Int64, value!: Int64): Unit {}
}

main() {
    var val = 1
    var a = A()
    (val, a[0]) = (2, 3)
    return val - 2
}
