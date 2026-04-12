// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A<T> {
    public static func add(a: A<T>, b: A<T>): Int64 {
        1
    }
}

main() {
    var a = A<Int64>()
    var b = A<Int64>()
    if (A.add(a, b) != 1) {
        return 1
    }
    return 0
}
