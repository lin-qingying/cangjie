// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C {
    operator func ()(a: Int32) {}
}

main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    1 |> C()
    err |> err

    (err |> err) + false
    0
}
