// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {}

class D<T> {}

func bar<<!CONFLICTING_UPPER_BOUNDS!>T<!>, U>() where T <: D<U> & D<I>, U <: I {}

main(): Int64 {
    return 1
}
