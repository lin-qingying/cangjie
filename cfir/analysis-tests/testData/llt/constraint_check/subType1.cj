// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<U> {}

open class Data<T> <: I<T> {}

class Data2<T> <: Data<T> {}

main() {
    var a: I<Int32> = Data<Int32>()
    var b: I<Int32> = Data2<Int32>()
    var c: Data<Int32> = Data2<Int32>()
    return 0
}
