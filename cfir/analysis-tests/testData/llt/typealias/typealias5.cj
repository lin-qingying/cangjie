// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type F<T, U> = (T) -> U

type U<X, Y> = F<X, Y>

func foo(): U<Int32, Bool> {
    return {<!chir_dce_unused_variable!>i<!>: Int32 => true}
}

main() {
    return 0
}
