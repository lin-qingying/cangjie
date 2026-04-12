// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var count: Int64 = 0

main(): Int64 {
    let f: Future<Unit> = spawn {
        => let fut: Future<Int64> = spawn {
            =>
            count = count + 1
            return 0
        }
    }
    return 0
}
