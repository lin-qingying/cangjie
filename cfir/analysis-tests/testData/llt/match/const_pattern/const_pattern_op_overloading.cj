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

main(): Int64 {
    let a = A()
    let b = A()

    let x: Int64 = match (a) {
        case b => 0
        case _ => 1
    }

    return x;
}
