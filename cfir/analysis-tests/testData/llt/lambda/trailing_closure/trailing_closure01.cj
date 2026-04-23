// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    func f(a: Int64, b: Int64, g: (Int64, Int64) -> Int64) {
        return g(a, b)
    }
    var t = f(1, 2) {x, y => x + 2 * y}
    return t - 5
}
