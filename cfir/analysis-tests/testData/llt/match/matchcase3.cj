// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 4
    var b = match (<!NON_EXHAUSTIVE_MATCH!>m<!>) {
        case 1 => 1
        case 2 => 2
        case 3 => 3
    }
    return b
}
