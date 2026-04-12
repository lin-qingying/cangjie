// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

internal import std.collection.*

func containsAll<T, C>(left: Set<T>, right: C): Bool where T <: Equatable<T>, C <: Collection<T> {
    for (v in right) {
        if (!(left.contains(v))) {
            return false
        }
    }
    true
}

main() {
    if (containsAll<Int64, Array<Int64>>(HashSet<Int64>(), [])) {
        return 0
    }
    return 1
}
