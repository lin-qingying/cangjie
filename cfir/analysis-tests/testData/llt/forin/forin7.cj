// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var sum = 0
    var n: Int8 = 127
    for (i in 0..=n) {
        println(i)
        sum += Int64(i)
    }
    if (sum != 8128) {
        return 1
    }
    return 0
}
