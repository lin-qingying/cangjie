// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: p1
// EXEC: %compiler %cmp_opt %compile_lib_opt ./p1/*.cj -o a.%middle
// EXEC: %compiler %cmp_opt a.%middle %f -o %output
// EXEC: %run %run_opt %output %run_args

//brief:导入泛型类型后继承

internal import p1.A
internal import p1.B as Boo

main(): Int64 {
    return 0
}

class C<T> <: A<T> {
    public var a: T
    public init(x: T) {
        a = x
    }
}

class D<T> <: Boo<T> {
    public init(x: T) {
        super(x)
    }
}
