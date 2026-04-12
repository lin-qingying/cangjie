// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main() {
    let a0: Array<UInt8> = "".toArray()
    if (a0.size != 0) {
        return 1
    }
    let a1 = "".toArray()
    if (a1.size != 0) {
        return 1
    }
    let a2 = "a".toArray()
    if (a2.size != 1 || a2[0] != 97) {
        return 1
    }
    let a3 = "\n".toArray()
    if (a3.size != 1 || a3[0] != 10) {
        return 1
    }
    let a5 = "\0".toArray()
    if (a5.size != 1 || a5[0] != 0) {
        return 1
    }
    let a6 = "a\n\r".toArray()
    if (a6.size != 3 || a6[0] != 97 || a6[1] != 10 || a6[2] != 13) {
        return 1
    }
    let a7 = "\nfd4.,$6!)(*?~`[]{}sa".toArray()
    let t7: Array<UInt8> = [10, 102, 100, 52, 46, 44, 36, 54, 33, 41, 40, 42, 63, 126, 96, 91, 93, 123, 125, 115, 97]
    if (a7.size != t7.size) {
        return 1
    }
    var i = 0
    while (i < a7.size) {
        if (a7[i] != t7[i]) {
            return 1
        }
        i++
    }
    let a8 = "\t\b\r\n\"\'\f\v\0\\".toArray()
    let t8: Array<UInt8> = [9, 8, 13, 10, 34, 39, 12, 11, 0, 92]
    i = 0
    while (i < a8.size) {
        if (a8[i] != t8[i]) {
            return 1
        }
        i++
    }

    return 0
}
