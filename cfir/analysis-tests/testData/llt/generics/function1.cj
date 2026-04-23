// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func Add<T>(a: T): T {
    return a
}

main() {
    var a: Int64 = 1
    Add<Float64>(<!TYPE_MISMATCH!>a<!>)
    Add<Int64>(a)
    Add<<!UNRESOLVED_REFERENCE!>T<!>>(a)
    Add(1)
}
