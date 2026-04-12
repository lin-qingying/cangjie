// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main() {
    let v1 = match (if (true) {
        func func1<T0>(): Int64 {
            Int64(1)
        };
        func1<Int8>();
        0
    } else {
        -1
    }) {
        case a: Equatable<Int64> =>
            if (a == 0) {
                0
            } else {
                -1
            }
        case _ => 1
    }
    let v2 = match (if (true) {
        func func1<T0>(): Int64 {
            Int64(1)
        };
        func1<Int8>();
        v1
    } else {
        v1
    }) {
        case a: Int64 => 0
        case _ => 1
    }
    v2
}
