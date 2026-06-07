// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var a = 24
    var b = 3
    a /= 7
    b **= 4
    if (a == 3 && b == 81) {
        return 0
    }
    return 1
}
