// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f(x: Int64): Any {
    if (x == 0) {
    } else if (x == 1) {
        true
    } else {
        throw Exception("")
    }
}

main() {
    let _ = f(0)
}
