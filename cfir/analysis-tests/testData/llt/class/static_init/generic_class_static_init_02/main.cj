// DEPENDENCE: pkg.cj pkg1.cj pkg2.cj
internal import pkg.*
internal import pkg1.*
internal import pkg2.*

main() {
    let _ = (A<Int64>(), A<Rune>())
    if (initCount != 1) {
        return 1
    }
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
