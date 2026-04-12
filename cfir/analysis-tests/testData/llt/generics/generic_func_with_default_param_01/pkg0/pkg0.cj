// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package test.pkg0

public const func foo<T0>(): UInt32 {
    const func my<T0, T1>(param1: T1, param2!: Int32 = 49): UInt32 {
        196
    }
    my<Bool, Rune>(r'O')
    0
}

public const func bar(): Unit {
    foo<UInt32>()
    ()
}
