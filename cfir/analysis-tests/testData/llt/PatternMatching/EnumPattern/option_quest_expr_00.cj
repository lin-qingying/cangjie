// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package core

struct Foo {
    let foo: Int64 = 17
}

enum Opt1on<T> {
    | Some(T)
}

main() {
    let x = Option.Some(Foo())
    let t = x?.foo
}
