// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// Test for compilation of instantiation
open class I1<F, K> {
    static func f3(a: F, b: K) {
        return 11
    }
}

class I2<K> <: I1<Int64, K> {}

main() {
    let a = I2.f3(1, Int32(1))
    if (a != 11) {
        return 1
    }
    return 0
}
