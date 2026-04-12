// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

let emptyArray: Array<Int64> = []
let list1 = [1 + 3, 2 + 3, 3 + 3]
let list3 = [4.0, 5.0, 6.0]
let list5 = ["a", "b", "c"]

main() {
    let listZero: Array<Int32> = []
    let listOne: Array<Int32> = [1, 2, 3, 4]
    var listTwo: Array<Int64> = [5, 6, 7]
    var listThree: Array<Float32>
    listThree = [3.14, 4.14, 5.14]
    var listFour = ["A", "B", "C"]
    var listFive = listThree
    let nestedArray = [[1, 2, 3, 4], [5, 6, 7]]
    return 0
}
