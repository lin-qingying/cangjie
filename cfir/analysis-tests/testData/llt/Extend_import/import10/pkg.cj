// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface I {
    func f(): Int64
}

extend Int64 <: I {
    public func f(): Int64 {
        return 111
    }
}

public func fff(x: Array<I>) {
    x[0].f()
}

public func ggg(x: I) {
    x.f()
}
