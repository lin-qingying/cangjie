// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

private const func foo() {
    return MemberVariable
}

private const func bar<T>(a: T) {
    return Type
}

private const b = Init

@Annotation[target: [foo(), bar(1), b]]
public class B {
    public const init() {}
}
