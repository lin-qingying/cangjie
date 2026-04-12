// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs0018002a

internal import pkgs0018001a.*

public func Bar<T>(a: T): T {
    return Foo<T>(a)
}
