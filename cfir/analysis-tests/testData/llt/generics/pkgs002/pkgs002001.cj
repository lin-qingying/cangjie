// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkgs002001a
// Test condition: package with several generic functions is imported

internal import pkgs002001a.Bar

main(): Int64 {
    var a: Int32 = 3
    var b = Bar<Int32>(a)
    if (b == 3) {
        return 0
    } else {
        return 1
    }
}
