// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj

internal import pkg.*

main() {
    let obj = A<Int64>()
    if (obj.test(123) != 123) {
        return 1
    }
    if (obj.foo(234) != 234) {
        return 2
    }
    let c = C()
    if (c.foo("23") != "23") {
        return 3
    }
    if (test() != 0) {
        return 4
    }
    return 0
}
