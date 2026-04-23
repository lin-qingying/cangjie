func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(a : T) : Bool where T <: Array<Bool> {
    if (a.isEmpty()) {
        return false
    }
    return a[0]
}

main() {
    if (!test([true])) {
        return 1
    }
    return 0
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
