// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    if (case01() != 4) {
        return 1
    }
    return 0
}

func case01() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    let a: Int32 = 0
    for (item in intList where item > 3) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>a<!> = item
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>item++<!>
    }
    a
}
