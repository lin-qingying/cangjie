// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    let scoreTuple = (("Allen", 80), ("Bob", 90))
    let allen = "Allen"
    let num80 = 80
    var scoreResult: String = match (scoreTuple) {
        case ((allen, num80), (bob, num90)) => "${bob} got ${num90}"
        case (_, _) => ""
    }
    print(scoreResult)
    0
}
