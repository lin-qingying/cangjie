// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum My {
    A |B(Int64) | C(Int64)
}

main() {
    let a = A

    if (let B(value) <- a <!parse_expected_if_let_andand!>where<!> value > 0) {
        value + 1
    } else { -1 }
}
