// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj

import pkg.*

main() {
    A1<Int64>.foo<Int64>()
    if (test() != 0) {
        return 4
    }
    A1<Int16>.foo<Rune>()
    return 0
}
