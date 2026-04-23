// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public interface H {}

sealed interface I <: H {}

class A <: I {}

class B {}

extend B <: I {}

func f(x: I) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case _: A => 0
        case _: B => 1
        case _: ToString => -1 // unreachable
    }<!>
}

main() {
    f(A())
}
