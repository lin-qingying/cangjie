// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct B {
    public static let d: Int64 = 1
    public let lambda: () -> Int64 = {=> d}
}

main(): Int64 {
    if (B().lambda() == 1) {
        return 0
    }
    return 1
}
