// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 2
    var b: Int32 = match {
        case m == 1 => 1
        case m == 2 => <!TYPE_MISMATCH!>2.1<!>
        case _ => 3
    }
    return b
}
