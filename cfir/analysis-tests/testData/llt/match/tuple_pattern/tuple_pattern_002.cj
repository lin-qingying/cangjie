// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (<!NON_EXHAUSTIVE_MATCH!>scoreTuple<!>) {
        case ("Bob", 90) => "Bob got 90"
        case (_, 100) => "someone got 100"
    }
    0
}
