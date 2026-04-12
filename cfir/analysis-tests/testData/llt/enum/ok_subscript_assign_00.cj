// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan [0, 2, 3]

enum E {
    Arr(Array<Int64>)

    operator func [](idx: Int64, value!: Int64) {
        match (this) {
            case Arr(arr) => arr[idx] = value
        }
    }
}

main() {
    let e = Arr([1, 2, 3])
    e[0] = 0
    match (e) {
        case Arr(arr) => println(arr)
    }
}
