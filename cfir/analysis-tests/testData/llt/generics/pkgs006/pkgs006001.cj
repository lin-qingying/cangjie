// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// Test condition: multi-file package with several generic functions along with normal function is imported

internal import pkgs006001a.NormalFunc
internal import pkgs006001a.Bar

main(): Int64 {
    var a: Int32 = 3
    var b = NormalFunc(a)
    var c: Int32 = 4
    var d = Bar<Int32>(c)

    if (b == 3 && d == 4) {
        return 0
    } else {
        return 1
    }
}
