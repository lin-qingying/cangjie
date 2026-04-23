// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public abstract class A {}

class B <: A {}

class C <: A {}

func f(x: A) {
    <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case _: B => 0
        case _: C => 1
    }<!>
}

main() {
    f(B())
}
