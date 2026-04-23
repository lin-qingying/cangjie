// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E {
    Foo

    operator func ()(a: Int64) {}
}

enum EE {
    Foo(Rune)
}

main() {
    Foo(1)
    0
}
