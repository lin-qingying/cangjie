// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

internal import std.math.*
internal import std.collection.*
internal import std.random.*

func id(value: ?Any): ?Any {
    value
}

main(): Int64 {
    let v = id(2)
    let a = v ?? Object()
    if (a is Int64) {
        return 0
    }
    let x: ??Any = 11
    let b = (((x ?? 0) ?? 0) as Int64) ?? 0
    if (b != 11) {
        return 1
    }
    return 0
}
