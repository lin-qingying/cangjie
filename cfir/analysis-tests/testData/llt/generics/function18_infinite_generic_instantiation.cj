// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class Base<T> {}

func buildList<T>(n: Int32): Int64 {
    if (n > 0) {
        <!sema_generic_infinite_instantiation!>b<!>uildList<Base<T>>(n - 1)
    } else {
        buildList<Base<T>>(0)
    }
    return 0
}

main(): Int64 {
    return buildList<Int32>(10)
}
