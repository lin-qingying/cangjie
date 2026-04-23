// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {}

extend A {
    public operator func ==(right: A) {
        return true;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (x == (A(), 1)) {
        return 0;
    }
    return 1;
}
