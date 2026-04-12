// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

// Original case.
open class Klass {
    func Check(arg: Int8) {
    }
}

extend Klass {
    private func h() {}
}

extend Klass {
    private static func h(a: Int64) {} // OK
}

main(ina: Array<String>) {
    var kl: Klass
    0
}
