// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum E {
    | A
    | B
}

enum F {
    | A
    | B
}

main() {
    let x = E.A
    match (x) {
        case A => 0
        case <!PATTERN_NOT_MATCH!>F.B<!> => 1
    }
}
