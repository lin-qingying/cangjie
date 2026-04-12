// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix %run_args

class M {
    let size = 3
}

class S {
    let x = M()
}

class C<T> {
    C(let value: T) {}
}

extend<T> C<T> where T <: S {
    func f() {
        let y = value.x.size
        return y
    }
}

main(): Int64 {
    let a = C<S>(S())
    if (a.f() != 3) {
        return 1
    }
    return 0
}
