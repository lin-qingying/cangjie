// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC-PIPE-0: %run %run_opt %n.%suffix | compare %f


main() {
    var arr: Array<Int64> = [0, 1, 2]
    var arr2 = Array<Int64>(4, {i => i})
    var arr3 = Array<Int64>()
    if (arr3.size != 0) {
        return 1
    }
    var i = 0
    while (i < arr.size) {
        if (arr[i] != arr2[i]) {
            return 2
        }
        i++
    }
    i = 0
    for (e in arr) {
        if (e != arr2[i]) {
            return 3
        }
        i++
    }
    if (i != 3) {
        return 4
    }
    if (arr2.size != 4) {
        return 5
    }
    var arr1 = Array<Int64>(1, repeat: 1)
    var cpy = arr1.clone()
    cpy[0] = 2
    if (arr1[0] == cpy[0]) {
        return 6
    }
    return 0
}

/* SCAN
*/
