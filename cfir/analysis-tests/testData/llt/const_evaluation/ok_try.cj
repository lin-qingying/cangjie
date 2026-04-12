// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

const func f(x: Int64) {
    try {
        x
    } catch (_) {
        x + 1
    } finally {
        x - 1
    }
}

const x = f(0)

main() {
    x
}
