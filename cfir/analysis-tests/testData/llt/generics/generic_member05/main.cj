// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj

import pkg.*

main() {
    if (A1<Int64>.test(123) != 123) {
        return 1
    }
    if (A1<Int64>.foo(234) != 234) {
        return 2
    }
    if (C1.foo("23") != "23") {
        return 3
    }
    if (test() != 0) {
        return 4
    }
    if (A1<Int16>.test(123) != 123) {
        return 1
    }
    if (A1<Int16>.foo(234) != 234) {
        return 2
    }
    if (C1.foo('2') != '2') {
        return 3
    }
    return 0
}
