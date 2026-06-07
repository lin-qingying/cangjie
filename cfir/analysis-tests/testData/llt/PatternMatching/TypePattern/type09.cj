<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
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
        case c => match (<!sema_nonexhuastive_patterns!>d<!>) {
            case <!sema_unreachable_pattern!>j: Float64<!> => a
        }
        case _ => a
    }.g) {}
