// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var a = 0
let b = c()

class c {}

struct d {
    public init(e: Float32, f: c) {}
    public init() {
        match {
            case _ => d((<!TYPE_MISMATCH!>a++<!>), match {
                case _ => b
            })
        }
    }
}
