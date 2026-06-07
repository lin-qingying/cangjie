// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

@Annotation
public class A { const init(_: Int64) {} }

@A[{ =>
    <!ANNOTATION_CUSTOM_PLACE!>@A[0]<!> let x = 0
    x
}()]
class B {}

main() {}
