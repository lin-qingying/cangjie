// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f(_: (Float32) -> Unit) {}

class A {}

main() {
    f(
        {
            _: Float32 =>
            func g<T0>() where T0 <: A {}
            g<A>()
        }
    )
}
