// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum Color {
    Red | Blue | Green
}

func test(Green: Int32) {
    var a: Int32 = Green
    a
}

main() {
    var t = Green
    var res = match (t) {
        case Red => 2
        case Blue => 4
        case Green => 158
    }

    if (res != 158 || test(123) != 123) {
        return 1
    }

    return 0
}
