// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

public func f1() {
    [Int64(-20), [if (true) {
        const func f222<T0>(a: T0): T0 {
            a
        };
        f222(true)
        Int64(-20)
    } else {
        Int64(-20)
    }]]
}

main(): Int64 {
    return 0;
}
