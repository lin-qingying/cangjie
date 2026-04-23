// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a = 3
    var res = 0
    if (let 2 | 3 | 4 <- a) {
        res++
    }
    if (let 0 | 2 | -1 <- a) {
    } else {
        res++
    }
    res
}
