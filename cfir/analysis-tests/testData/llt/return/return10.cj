// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f(x!: Bool = if (true) {
        func g(x!: Bool = <!INVALID_RETURN!>return<!>) {
            return
        };
        <!INVALID_RETURN!>return true<!>
    } else {
        false
    }, y!: Bool = x) {
    return y
}

main() {
    let x: Bool = f()
}
