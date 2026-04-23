// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

private interface I {}

private class A <: I {}

private interface J <: I {}

private class B {}

extend B <: J {}

func f(x: Option<I>) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case Some(_: A) => 0
        case Some(_: J) => 1
        case Some(_: B) => -1 // unreachable
    }<!>
}

main() {
    f(Some(0))
}
