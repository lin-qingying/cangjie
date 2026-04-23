// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Test {
    public init() {
        this(true)
    }

    public <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>(a: Bool) {
        this(true)
    }
}

main(): Int64 {
    var test: Test = Test()
    return 0
}
