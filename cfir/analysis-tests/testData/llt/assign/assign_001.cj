// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo1(): Int32 {
    1
}

func foo2(): Int32 {
    2
}

main(): Int64 {
    <!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>foo1<!> = foo2
    return 0
}
