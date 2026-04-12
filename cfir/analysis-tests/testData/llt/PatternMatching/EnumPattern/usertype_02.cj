// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum E {
    | A(Int64)
    | A(Int64, Int64)
}

main() {
    let x = E.A(0)
    match (x) {
        case A(0) => 0
        case E.A(1) => 1
        case A(_) => 2
        case E.A(_, _) => 3
    }
}
