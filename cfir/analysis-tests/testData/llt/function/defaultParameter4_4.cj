// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
    return
}

main(): Int64 {
    foo0<!NO_VALUE_FOR_PARAMETER!>(a: 1)<!>
    foo0(1, <!NAMED_PARAMETER_NOT_FOUND!>d<!>: 2)
    foo0(1, <!NAMED_PARAMETER_NOT_FOUND!>d<!>: 2, e: 3)
    foo0(1, d: 2, e: 3, <!TOO_MANY_ARGUMENTS!>f: 4<!>)

    return 0
}
