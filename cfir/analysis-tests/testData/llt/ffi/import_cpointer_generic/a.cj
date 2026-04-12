// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package test.subpkg

public class XDArry<T> where T <: CType & ToString {
    let data_: CPointer<T>
    public prop data: CPointer<T> {
        get() {
            this.data_
        }
    }
    public init() {
        this.data_ = CPointer<T>()
    }
}
