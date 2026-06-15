<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


var a = b()
let c = true
var d = e

enum f {
    e
}

class b {
    public let g: Int8 = 5
}

func h(i!: Int8 = match {
        case c => match (<!NON_EXHAUSTIVE_MATCH!>d<!>) {
            case <!UNREACHABLE_PATTERN!>j: Float64<!> => a
        }
        case _ => a
    }.g) {}
