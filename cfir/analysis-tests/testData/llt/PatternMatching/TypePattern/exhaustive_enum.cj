// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E {
    | A(Bool, Int64)
    | B(Rune)
    | C
}

func f(x: E) {
    match (x) {
        case <!UNREACHABLE_PATTERN!>A(_: Int64, _: Int64)<!> => -1 // unreachable
        case A(_: Bool, _: Int64) => 0
        case B(_: ToString) => 1
        case C => 2
        case <!UNREACHABLE_PATTERN!>_<!> => -1 // unreachable
    }
}

main() {
    f(B(r'😂'))
}
