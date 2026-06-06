// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// ERRCHECK: %compiler %cmp_opt pkga.%middle %f -o %output

internal import <!package_search_error!>pkga<!>.*

class C <: I { // error
    public func f() {
        2
    }
}

interface J <: I {} // error

class D {}

extend D <: I { // error
    public func f() {
        3
    }
}

main() {}
