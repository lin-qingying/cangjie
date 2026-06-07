// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f<X>(a: X) where X <: I<X> & I<Int64> {}

interface I<X> {}

extend Bool <: I<Bool> {}

extend Bool <: I<Int64> {}

main() {
    f(true)
    0
}
