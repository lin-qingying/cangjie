// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj

internal import pkg.*

main() {
    if (A<Int64>.a != 1) {
        return 1
    }
    test()
    if (A<Int64>.a != 3) {
        return 2
    }
    return 0
}
