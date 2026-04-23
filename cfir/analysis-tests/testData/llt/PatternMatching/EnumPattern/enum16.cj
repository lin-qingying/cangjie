// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum A<T> {
    A1(T)
}

enum E {
    | E0
    | E1(Int64, (<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>A<!>, Bool))
}

main() {
    let x = E0
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case E0 => 0
    }<!>
}
