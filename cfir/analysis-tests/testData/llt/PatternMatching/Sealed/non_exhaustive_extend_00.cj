// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

sealed interface I {
    func f() {
        <!NON_EXHAUSTIVE_MATCH!>match (this) {
            case _: A => 1
        }<!>
    }
}

extend Int64 <: I {}

class A <: I {}

main() {
    1.f()
}
