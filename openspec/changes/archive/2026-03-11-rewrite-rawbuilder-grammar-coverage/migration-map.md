# rawBuilder Migration Map

Old path -> new path (relative to `cfir/raw-cfir/psi2cfir/testData/rawBuilder`):

- `declarations/emptyFile.*` -> `declarations/file-structure/emptyFile.*`
- `declarations/packageAndImport.*` -> `declarations/file-structure/packageAndImport.*`
- `declarations/emptyClass.*` -> `declarations/class-like/emptyClass.*`
- `declarations/classWithMembers.*` -> `declarations/class-like/classWithMembers.*`
- `declarations/classWithSupertype.*` -> `declarations/class-like/classWithSupertype.*`
- `declarations/classWithTypeParameters.*` -> `declarations/class-like/classWithTypeParameters.*`
- `declarations/classMembersOrderStability.*` -> `declarations/class-like/classMembersOrderStability.*`
- `declarations/interfaceDeclaration.*` -> `declarations/class-like/interfaceDeclaration.*`
- `declarations/structDeclaration.*` -> `declarations/class-like/structDeclaration.*`
- `declarations/enumDeclaration.*` -> `declarations/class-like/enumDeclaration.*`
- `declarations/topLevelFunction.*` -> `declarations/top-level/topLevelFunction.*`
- `declarations/topLevelProperty.*` -> `declarations/top-level/topLevelProperty.*`
- `declarations/typeAlias.*` -> `declarations/top-level/typeAlias.*`
- `declarations/functionExpressions.*` -> `expressions/basics/functionExpressions.*`
- `declarations/controlFlow.*` -> `control-flow/valid/controlFlow.*`
- `declarations/extendDeclaration.*` -> `cangjie-features/extend/extendDeclaration.*`
- `declarations/varrayTypeRef.*` -> `cangjie-features/varray/varrayTypeRef.*`
- `expressions/binary/binaryMissingRightOperand.*` -> `recovery/expressions/binaryMissingRightOperand.*`
- `expressions/control-flow/forMissingIterable.*` -> `recovery/control-flow/forMissingIterable.*`
- `expressions/control-flow/ifMissingCondition.*` -> `recovery/control-flow/ifMissingCondition.*`
- `expressions/control-flow/throwMissingExpression.*` -> `recovery/control-flow/throwMissingExpression.*`
- `expressions/control-flow/whileMissingCondition.*` -> `recovery/control-flow/whileMissingCondition.*`

New official-syntax tests introduced in this change:

- `declarations/top-level/mainEntryOfficial.cj`
- `cangjie-features/match/matchExpressionOfficial.cj`
- `cangjie-features/spawn/spawnExpressionOfficial.cj`
- `cangjie-features/extend/extendWithWhereOfficial.cj`
- `types/type-references/typeAliasRefsOfficial.cj`
- `expressions/basics/trailingClosureOfficial.cj`
