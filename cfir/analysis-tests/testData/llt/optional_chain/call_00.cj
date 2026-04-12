// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func foo(b: Int64) {
    b - 1
}

main() {
    let a = Some(foo)
    let b = 1
    let x = a?(b)
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
