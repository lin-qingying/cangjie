// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct A<T>{
    private func foo(input:T){}
    <!parse_expected_decl!>return<!> 1;    func test(input:T){
        var a : A<T> = A<T>()
        a(input)
   }
}

main() {
    var a : A<Int64> = A<Int64>()
    return 0
}
