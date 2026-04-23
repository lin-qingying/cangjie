// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj

internal import pkg.*

main() {
    var e = Add(Ele(1), Sub(Ele(2), Ele(3)))
    print(e)
    let r = Rcd<Int64>()
    print(r)
    return 0
}
