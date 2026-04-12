// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

#include <stdio.h>

struct S {
    int a[2][2][2][2];
    int b[3];
};

void printS(struct S s)
{
    printf("a map: ");
    int* pa = s.a;
    for (; pa != s.b; ++pa) {
        printf("%d ", *pa);
    }
    printf("\n");
    printf("b[0]: %d\n", s.b[0]);
}