// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo<T>() {
    1
}

func me<T>(a!: () -> Int64 = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>foo<!>) {
    0
}

main(): Int64 {
    return me<Int64>() - 1
}
