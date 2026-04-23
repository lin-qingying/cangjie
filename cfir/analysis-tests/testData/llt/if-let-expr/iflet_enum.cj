// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E {
    | A
    | A(Int64)
}

main() {
    var a = 1
    if (let A <- A(1)) {
        a++
    } else {
        a--
    }
    return a
}
