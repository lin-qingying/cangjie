// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func test(): Int64 {
    var a = 1
    var b = 2
    var res = match {
        case a > b =>
            var x = 0
            x + 1
        case a < b =>
            var x = a + b - 3
            x
        case _ => -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
