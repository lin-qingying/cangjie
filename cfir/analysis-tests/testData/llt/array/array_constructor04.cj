// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import std.core.Array as Arr

main() {
    let arr0 = Arr(3, repeat: 'c')
    let arr1 = Arr<Int8>(3, repeat: 3)
    let arr2: Arr<ToString> = Arr(3, repeat: 3)
    if (arr0[2] != 'c' || arr1[2] != 3) {
        return 1
    }
    if (arr2[2].toString() != "3") {
        return 2
    }
    return 0
}
