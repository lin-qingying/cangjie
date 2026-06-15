// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface EQ {}

class A<T> {}

interface B<T> {}

extend Int32 <: EQ {}

extend<T> A<T> <: B<T> where T <: EQ {}

main() {
    let a: B<Int32> = A<Int32>()
    let s: B<Int64> = <!TYPE_MISMATCH!>A<Int64>()<!>
    0
}
