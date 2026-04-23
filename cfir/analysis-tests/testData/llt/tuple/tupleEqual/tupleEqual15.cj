// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

class B {}

main(): Int64 {
    let x = (A(), B());
    if (x <!INVALID_BINARY_OPERATOR!>==<!> (A(), B())) {
        return 0;
    }
    return 1;
}
