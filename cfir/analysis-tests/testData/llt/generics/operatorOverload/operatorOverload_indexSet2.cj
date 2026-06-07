// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public operator func [](index: Int64, value!: Int64): <!INVALID_SUBSCRIPT_ASSIGN_RETURN!>Int64<!> {
        return 1;
    }
}

main(): Int64 {
    return 1;
}
