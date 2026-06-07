// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum TimeUnit1 {
    Year(String)
}

main(): Int64 {
    var str = Year("abc")
    var a = match (str) {
        case Year("x") => 1
        case Year("abc") => 0
        case _ => 2
    }
    return a
}
