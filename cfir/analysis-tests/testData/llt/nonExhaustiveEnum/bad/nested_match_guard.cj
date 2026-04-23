// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E {
    A | B(Int64) | ...
}

enum D {
    D1 | D2(Int64)
}

let k = (D1, B(10))

main() {
    <!NON_EXHAUSTIVE_MATCH!>match (k) {
        case (D1, A) => 1
        case (D1, B(_)) where true => 2
        case (D2(a), _) => a
    }<!>
}
