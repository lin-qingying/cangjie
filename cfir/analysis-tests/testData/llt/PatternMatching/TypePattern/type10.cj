// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

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
        case c => match (d) {
            case j: Float64 => a
            case _ => b()
        }
        case _ => a
    }.g) {
    i
}

main(): Int64 {
    return Int64(h() - 5)
}
