// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p1

interface I<T> {}

class C1 {}

class C2<T> <: I<T> {
    let x: C1 = C1()
}

public func f<T>() {
    let y: I<T> = C2<T>()
}
