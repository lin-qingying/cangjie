// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

func foo(): Option<Int64> {
    let x1 = Option<Int64>.Some(111)
    let x2 = Option<Int64>.None
    if (let Some(v) <- x1) {
        v
    } else if (let Some(v) <- x2) {
        v
    } else {
        None
    }
}

main() {
    let a = foo()
    if (let Some(v) <- a) {
        if (v != 111) {
            return 1
        }
    }
    0
}
