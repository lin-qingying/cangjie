// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum EE {
    A1 | B1(Int64)

    const func get() {
        match (this) {
            case A1 => 1
            case _ => 2
        }
    }
}

main() {
    A1.get()

    return 0
}
