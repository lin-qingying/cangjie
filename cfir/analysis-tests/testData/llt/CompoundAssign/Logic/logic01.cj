// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var a = true
    var b = false
    a &&= (1 > 2)
    b ||= (1 + 1 == 2)
    if (!a && b) {
        return 0
    }
    return 1
}
