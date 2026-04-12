// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    let arr1 = Array<Int64>(3) {i => i}
    if (arr1[2] != 2) {
        return 1
    }

    let arr2 = Array(3) {i: Int64 => Rune(48 + UInt32(i))}
    if (arr2[2] != r'2') {
        return 2
    }
    return 0
}
