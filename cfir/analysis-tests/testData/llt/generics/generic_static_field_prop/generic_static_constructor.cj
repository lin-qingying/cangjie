// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)ERRCHECK: %frontendCompiler %typecheck %cmp_opt %f %diag_json
// (CJNATIVE)ASSERT: scan sema_static_variable_use_generic_parameter
// (not CJNATIVE) EXEC: %compiler %f

class C {
    static func test() {
        0
    }
}

class A<T> where T <: C {
    static init() {
        T.test()
    }
}

main() {}
