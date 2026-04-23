// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var var1: (Int32, Int8) = (1, 2)
    if (((1, 2), 3)[0] != var1) {
        return 1
    }
    if (!(((1, 2), 3)[0] == var1)) {
        return 2
    }
    return 0
}
