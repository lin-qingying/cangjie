// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A<T> {
    init(x: Bool, a: Array<T>) {}
    static func f(x: Array<T>) {
        x.size
    }
}

main() {
    let x = <!sema_object_cannot_access_static_member!>A<!><Int64>(true).f() == 0
}
