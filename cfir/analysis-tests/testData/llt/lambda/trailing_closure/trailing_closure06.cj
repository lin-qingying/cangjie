// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    func f(a: Int64) {
        func g(h: (Int64) -> Int64) {
            h(0) - a
        }
        g
    }
    var t = <!TOO_MANY_ARGUMENTS!>f(1) {t => t + 1}<!>
    return 0
}
