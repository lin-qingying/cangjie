// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>

char** testfunc() {
    char* str1 = (char*)malloc(sizeof(char) * 5);
    str1[0] = 'c';
    str1[1] = 'a';
    str1[2] = 'n';
    str1[3] = 'g';
    str1[4] = '\0';
    char* str2 = (char*)malloc(sizeof(char) * 4);
    str2[0] = 'j';
    str2[1] = 'i';
    str2[2] = 'e';
    str2[3] = '\0';
    char** ptr = (char**)malloc(sizeof(char*) * 2);
    ptr[0] = str1;
    ptr[1] = str2;
    return ptr;
}

void freeMalloced(char** ptr) {
    free(ptr[0]);
    free(ptr[1]);
    free(ptr);
}
