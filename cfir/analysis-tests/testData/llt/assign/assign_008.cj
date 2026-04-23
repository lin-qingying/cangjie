// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f() {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>E.A<!> = E.A
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A<!> = A
}

enum E {
    A
}

main() {}
