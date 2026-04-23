// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum ET {
    A | B | C

    public func f<U>(a: U) {
        return a
    }
}

main() {
    let a = ET.A
    return a.f<Int64>(0)
}
