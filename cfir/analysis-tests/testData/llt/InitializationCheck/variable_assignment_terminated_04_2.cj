// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f(c: Bool) {
    let a: Int64
    if (c) {
        try {
            a = Some(1) ?? throw Exception()
            a = 2
            println(a)
        } catch (_) {
        }
    } else {
        a = 2
    }
    return <!USED_BEFORE_INITIALIZATION!>a<!>
}

main() {
    f(true) - 1
}
