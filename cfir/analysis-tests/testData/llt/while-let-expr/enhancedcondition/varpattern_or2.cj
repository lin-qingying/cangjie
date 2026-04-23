// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a = Some(15)
    while (<!VAR_IN_OR_CONDITION!>false || let Some(b) <- a || let Some(c) <- a<!>) {
        return b - 1
    }

    return 2
}

enum A {
    B | C(Int32) | D(Int64)
}

func foo(a: A) {
    while (<!VAR_IN_OR_CONDITION!>let B | C(v) | D(d) <- a || false<!>) {
        1
    }
}
