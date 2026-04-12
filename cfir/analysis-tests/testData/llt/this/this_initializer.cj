// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %f %compile_lib_opt

import std.collection.*

public interface FieldPathComparableExpansion<F> {
    func compare(): Ordering
}

extend<K> ArrayList<K> <: FieldPathComparableExpansion<K> {
    public func compare() {
        let leftValues = this
        Ordering.GT
    }
}
