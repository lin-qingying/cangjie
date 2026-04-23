// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj

internal import pkga.*

abstract sealed class C {
    public open func f() {
        0
    } // no warning
}

func f(x: A) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case b: B => b.f()
    }<!>
}

main() {
    f(B())
}
