// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// Test condition: package with a single generic function is imported

internal import pkgs001001a.foo

main(): Int64 {
    let res = foo<Int32>(3)
    if (res == 3) {
        return 0
    } else {
        return 1
    }
}
