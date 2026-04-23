// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface IA {
    mut prop x: Int64
}

class A<T> where T <: IA {
    let t: T
    init(t: T) {
        this.t = t
    }
    func foo() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>t.x<!> = t.x + 1
    }
}

main() {}
