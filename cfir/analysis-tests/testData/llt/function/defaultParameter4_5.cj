// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo0(a: Int32, b!: Int32 = 2, c!: Int32): Unit {
    return
}

main(): Int64 {
    foo0<!NO_VALUE_FOR_PARAMETER!>()<!>
    foo0<!NO_VALUE_FOR_PARAMETER!>(1)<!>
    foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>)
    foo0(1, <!NEED_NAMED_ARGUMENT!>2<!>, 3)
    foo0(1, b: 2, c: 3, <!TOO_MANY_ARGUMENTS!>4<!>)
    return 0
}
