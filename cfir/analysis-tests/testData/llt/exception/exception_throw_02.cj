// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

public func assertThrown<T>(fn: () -> Int64, msg!: String = ""): Int64 where T <: Exception {
    try {
        return fn()
    } catch (e: T) {
        if (!(e is T)) {
            throw e
        }
    }
    return 0
}

main(): Int64 {
    let a = assertThrown<Exception>({=> 11})
    if (a != 11) {
        return 1
    }
    return 0
}
