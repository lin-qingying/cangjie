# Gradle module catalog

This catalog is the maintained navigation map for every module included by `settings.gradle.kts`. The build task `validateDocumentation` verifies that the module identifiers below exactly match that file.

Module-specific implementation notes belong to the linked subsystem document. Internal leaves and publication artifacts are documented through their owning subsystem instead of duplicating a README in every directory.

<!-- module-catalog:start -->

| Gradle module | Subsystem | Responsibility | Owner documentation |
| --- | --- | --- | --- |
| `:analysis:analysis-api` | Analysis | Public analysis API | [owner](../analysis/README.md) |
| `:analysis:analysis-api-cfir` | Analysis | CFIR-backed analysis implementation | [owner](../analysis/README.md) |
| `:analysis:analysis-api-cfir:analysis-api-cfir-generator` | Analysis | Analysis API source generator | [owner](../analysis/README.md) |
| `:analysis:analysis-api-impl-base` | Analysis | Shared analysis implementation base | [owner](../analysis/README.md) |
| `:analysis:analysis-api-platform-interface` | Analysis | Platform contract for analysis | [owner](../analysis/README.md) |
| `:analysis:analysis-api-standalone` | Analysis | Standalone analysis runtime | [owner](../analysis/README.md) |
| `:analysis:analysis-internal-utils` | Analysis | Analysis-internal utilities | [owner](../analysis/README.md) |
| `:analysis:analysis-test-framework` | Analysis | Analysis test fixtures | [owner](../analysis/README.md) |
| `:analysis:analysis-tools` | Analysis | Analysis command-line tools | [owner](../analysis/README.md) |
| `:analysis:cj-references` | Analysis | References and navigation support | [owner](../analysis/README.md) |
| `:analysis:decompiled` | Analysis | Decompilation aggregation | [owner](../analysis/README.md) |
| `:analysis:decompiled:decompiler-to-file-stubs` | Analysis | CJO file-stub decompiler | [owner](../analysis/README.md) |
| `:analysis:decompiled:decompiler-to-psi` | Analysis | CJO PSI decompiler | [owner](../analysis/README.md) |
| `:analysis:decompiled:decompiler-to-stubs` | Analysis | CJO stub-tree decompiler | [owner](../analysis/README.md) |
| `:analysis:decompiled:light-declarations-for-decompiled` | Analysis | Decompiled light declarations | [owner](../analysis/README.md) |
| `:analysis:light-declarations` | Analysis | Light declaration model | [owner](../analysis/README.md) |
| `:analysis:low-level-api-cfir` | Analysis | Lazy CFIR analysis services | [owner](../analysis/README.md) |
| `:analysis:stubs` | Analysis | Stub model and indices | [owner](../analysis/README.md) |
| `:analysis:symbol-light-declarations` | Analysis | Symbol-backed light declarations | [owner](../analysis/README.md) |
| `:cfir` | CFIR | CFIR aggregation | [owner](../cfir/README.md) |
| `:cfir:analysis-tests` | CFIR | CFIR end-to-end analysis tests | [owner](../cfir/README.md) |
| `:cfir:cfir-common` | CFIR | CFIR sessions and shared model | [owner](../cfir/README.md) |
| `:cfir:cfir-cones` | CFIR | CFIR type system | [owner](../cfir/README.md) |
| `:cfir:cfir-serialization` | CFIR | CJO serialization support | [owner](../cfir/README.md) |
| `:cfir:cfir-tree` | CFIR | Generated CFIR tree | [owner](../cfir/README.md) |
| `:cfir:cfir-tree:tree-generator` | CFIR | CFIR tree generator | [owner](../cfir/README.md) |
| `:cfir:checkers` | CFIR | Diagnostic checker framework | [owner](../cfir/README.md) |
| `:cfir:checkers:checkers-component-generator` | CFIR | Checker component generator | [owner](../cfir/README.md) |
| `:cfir:diagnostic-renderers` | CFIR | Diagnostic renderers | [owner](../cfir/README.md) |
| `:cfir:entrypoint` | CFIR | CFIR frontend assembly | [owner](../cfir/README.md) |
| `:cfir:providers` | CFIR | Symbol and extension providers | [owner](../cfir/README.md) |
| `:cfir:raw-cfir` | CFIR | Raw CFIR construction aggregation | [owner](../cfir/README.md) |
| `:cfir:raw-cfir:light-tree2cfir` | CFIR | LightTree to Raw CFIR | [owner](../cfir/README.md) |
| `:cfir:raw-cfir:psi2cfir` | CFIR | PSI to Raw CFIR | [owner](../cfir/README.md) |
| `:cfir:raw-cfir:raw-cfir-common` | CFIR | Raw CFIR shared abstractions | [owner](../cfir/README.md) |
| `:cfir:resolve` | CFIR | CFIR semantic resolution | [owner](../cfir/README.md) |
| `:cfir:semantics` | CFIR | Shared semantic utilities | [owner](../cfir/README.md) |
| `:chir` | CHIR | CHIR aggregation | [owner](../chir/README.md) |
| `:chir:cfir2chir` | CHIR | CFIR to CHIR lowering | [owner](../chir/README.md) |
| `:chir:chir-tree` | CHIR | CHIR tree and passes | [owner](../chir/README.md) |
| `:code-insight` | Code insight | Editor-service aggregation | [owner](../code-insight/README.md) |
| `:code-insight:api` | Code insight | Editor-service API | [owner](../code-insight/README.md) |
| `:code-insight:fixes` | Code insight | Editor quick fixes | [owner](../code-insight/README.md) |
| `:code-insight:folding` | Code insight | Editor folding | [owner](../code-insight/README.md) |
| `:code-insight:formatting` | Code insight | Editor formatting | [owner](../code-insight/README.md) |
| `:code-insight:highlighting` | Code insight | Editor highlighting | [owner](../code-insight/README.md) |
| `:code-insight:override-implement` | Code insight | Override and implement actions | [owner](../code-insight/README.md) |
| `:code-insight:refactoring` | Code insight | Editor refactoring | [owner](../code-insight/README.md) |
| `:common` | Common | Shared compiler domain model | [owner](../common/README.md) |
| `:common:diagnostics` | Common | Shared diagnostics framework | [owner](../common/README.md) |
| `:compiler` | Compiler | Compiler driver aggregation | [owner](../compiler/README.md) |
| `:compiler:arguments` | Compiler | Compiler CLI arguments | [owner](../compiler/README.md) |
| `:compiler:codegen` | Compiler | CHIR to LLVM code generation | [owner](../compiler/README.md) |
| `:compiler:config` | Compiler | Compiler configuration | [owner](../compiler/README.md) |
| `:compiler:frontend` | Compiler | Frontend pipeline coordination | [owner](../compiler/README.md) |
| `:compiler:frontend-arguments-generator` | Compiler | CLI argument generator | [owner](../compiler/README.md) |
| `:compiler:jvm-codegen` | Compiler | CHIR to JVM code generation | [owner](../compiler/README.md) |
| `:compiler:phaser` | Compiler | Compiler phase framework | [owner](../compiler/README.md) |
| `:compiler:plugin` | Compiler | Compiler plugin integration | [owner](../compiler/README.md) |
| `:dependencies:intellij-core` | Dependencies | IntelliJ platform dependency boundary | [owner](../README.md) |
| `:flatbuffers-gen` | Infrastructure | FlatBuffers schemas and generated protocol sources | [owner](../flatbuffers-gen/README.md) |
| `:generators` | Infrastructure | Code-generation framework | [owner](../generators/README.md) |
| `:llvm-interop` | LLVM interop | LLVM interop aggregation | [owner](../llvm-interop/README.md) |
| `:llvm-interop:llvm-interop-api` | LLVM interop | LLVM interop API | [owner](../llvm-interop/README.md) |
| `:llvm-interop:llvm-interop-jni` | LLVM interop | LLVM JNI implementation | [owner](../llvm-interop/README.md) |
| `:lsp` | Language server | Language Server framework | [owner](../lsp/README.md) |
| `:macro:macro-common` | Macro | Macro protocol and shared API | [owner](../macro/README.md) |
| `:macro:macro-process` | Macro | External macro executor | [owner](../macro/README.md) |
| `:macro:macro-stub` | Macro | Macro test and IDE stub | [owner](../macro/README.md) |
| `:prepare:analysis-test-framework` | Publication | Published frontend support artifact | [owner](../prepare/README.md) |
| `:prepare:frontend` | Publication | Published frontend support artifact | [owner](../prepare/README.md) |
| `:prepare:frontend-embeddable` | Publication | Published frontend support artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-cfir-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-standalone-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-cfir-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-folding-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-formatting-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-highlighting-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-code-insight-refactoring-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-common-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies-module:cangjie-frontend-psi-for-ide-module` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-cfir-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-analysis-api-standalone-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-cfir-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-folding-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-formatting-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-highlighting-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-code-insight-refactoring-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-common-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:ide-plugin-dependencies:cangjie-frontend-psi-for-ide` | Publication | IDE dependency publication artifact | [owner](../prepare/README.md) |
| `:prepare:test-infrastructure` | Publication | Published frontend support artifact | [owner](../prepare/README.md) |
| `:psi` | Syntax | Lexer, parser, and PSI | [owner](../psi/README.md) |
| `:resolution.common` | Resolution | Shared inference and call resolution | [owner](../resolution.common/README.md) |
| `:tests` | Test infrastructure | Test-infrastructure aggregation | [owner](../tests/README.md) |
| `:tests:test-infrastructure` | Test infrastructure | Shared test framework | [owner](../tests/README.md) |
| `:util` | Infrastructure | General compiler utilities | [owner](../util/README.md) |

<!-- module-catalog:end -->

