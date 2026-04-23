// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(): Int32 {
    let x: Int64 = 1
    let flag: Bool = true
    var b: Int32 = match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case _ where flag => 1
        case _ where false => 2
    }
    return b
}
