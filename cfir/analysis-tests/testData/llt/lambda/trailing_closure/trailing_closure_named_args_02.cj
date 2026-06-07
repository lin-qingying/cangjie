// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    func g(h!: (Int64) -> Int64, a!: (Int64) -> Int64) {
        h(0) - a(2)
    }
    var t = g(a: {t => t + 1}) <!ARGUMENT_PASSED_TWICE!>{<!>t => t + 1}
    return 0
}
