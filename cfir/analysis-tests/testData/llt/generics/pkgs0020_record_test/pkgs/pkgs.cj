// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs

struct Data<T> {
    public let width: T
    public let length: T
    public init(width1: T, length1: T) {
        width = width1
        length = length1
    }
    public func area(): T {
        return width
    }
}
