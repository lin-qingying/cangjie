// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public var a: Int32 = 3
    ~init() {}
}

main() {
    if (A().a == 3) {
        return 0
    }
    return 1
}
