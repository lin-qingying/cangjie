// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

type B<T> = <!UNDECLARED_TYPE_NAME!>Cl<!><T>

func run<T>(a: T): Int64 {
    var b: B<T>
    return 0
}

main(): Int64 {
    return run<Int64>(1)
}
