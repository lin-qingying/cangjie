# Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
# This source file is part of the Cangjie project, licensed under Apache-2.0
# with Runtime Library Exception.
#
# See https://cangjie-lang.cn/pages/LICENSE for license information.

# Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
# This source file is part of the Cangjie project, licensed under Apache-2.0
# with Runtime Library Exception.
#
# See https://cangjie-lang.cn/pages/LICENSE for license information.

# Generate cangjie code for libast using flatc with cangjie backend.
set(FLATC_EXECUTABLE "${CMAKE_BINARY_DIR}/bin/flatc")

set(FLATBUFFERS_SOURCE
    ModuleFormat.fbs
    CachedASTFormat.fbs
    NodeFormat.fbs)

list(APPEND FLATBUFFERS_SOURCE BCHIRFormat.fbs PackageFormat.fbs)
list(APPEND FLATBUFFERS_SOURCE MacroMsgFormat.fbs)

set(FLATC_OUTPUTS)
foreach(FILE ${FLATBUFFERS_SOURCE})
    get_filename_component(FILENAME ${FILE} NAME_WE)
    set(FLATC_OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/flatbuffers/${FILENAME}_generated.h")

    add_custom_command(
        OUTPUT ${FLATC_OUTPUT}
        COMMAND ${FLATC_EXECUTABLE} ARGS --no-warnings -c -o "${CMAKE_CURRENT_BINARY_DIR}/flatbuffers" ${FILE}
        DEPENDS ${FILE} flatbuffers # flatc defined in flatbuffers project: add_executable(flatc..)
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
    list(APPEND FLATC_OUTPUTS ${FLATC_OUTPUT})
endforeach()

set(FLATBUFFERS_INCLUDE_DIR
    ${CMAKE_CURRENT_BINARY_DIR} ${CMAKE_BINARY_DIR}/include
    PARENT_SCOPE)
add_custom_target(CangjieFlatbuffersHeaders DEPENDS ${FLATC_OUTPUTS} ${FLATC_LIBAST_OUTPUTS})
