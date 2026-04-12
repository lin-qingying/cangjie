// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// RUN-EXEC: %run %run_opt %output %run_args

func test<T>(a: (Int64, T)) {
    match (a) {
        case (_, v: Int64) => v
        case _ => 0
    }
}

main() {
    let val = 22
    let v = test((1, val))
    if (v != val) {
        return 1
    }
    return 0
}
