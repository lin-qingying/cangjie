// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkg.cj

internal import pkg.*

func mediator(f: Interaction) {
    <!NON_EXHAUSTIVE_MATCH!>match (f) {
        case _: Weak => "W and Z bosons"
        case _: Strong => "gluons"
        case _: Gravity => "gravitons"
        case _: Electromagnetic => "photons"
    }<!>
}

main() {
    println(mediator(Gravity()))
}
