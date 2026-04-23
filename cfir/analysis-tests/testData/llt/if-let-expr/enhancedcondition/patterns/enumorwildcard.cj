// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum A {
    | B | C(Int64) | ...
}

var a = B

main() {
    while (let <!DIFFERENT_OR_PATTERN!>B | _<!> <- a) {
        break
    }
    if (let <!DIFFERENT_OR_PATTERN!>B | _<!> <- a) {
        0
    } else {
        1
    }
}
