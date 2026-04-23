// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

extend<T> Array<T> {
    public func reduce<Tnew>(cb: ((Array<T>) -> T), i: Tnew) {
        var y: Tnew = i
        for (i in 0..this.size) {
            y = <!TYPE_MISMATCH!>cb(this)<!>
        }
        y
    }
}

main() {}
