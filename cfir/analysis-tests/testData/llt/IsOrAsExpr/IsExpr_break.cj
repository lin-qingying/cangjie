// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    let arr: Array<Int64> = [0, 1, 2, 3, 4]
    for (i in arr) {
        if (i == 1) {
            break is Int64
        }
        print("${i}")
    }
    return 0
}
