// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1<T> {
    mut func foo1(x: T) {
        return x
    }
}

struct R1<T> <: I1<T> {
    public mut func foo1(x: T) {
        return x
    }
}

main() {
    return 0
}
