// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-2: %run %run_opt %output %run_args

enum E {
    A
    | A(Array<Int64>)

    operator func ()(x: Array<Int8>, a!: Int64, b!: Array<Int32>) {
        x.size + a + b.size
    }
}

main() {
    A(1, 2, a: 0, b: [])
}
