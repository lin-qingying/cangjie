package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.name.FqName

/** `quote` 表达式所需的标准库包（官方 `QuoteExpr.cpp:26` 的诊断参数 `std.ast`）。 */
val AST_PACKAGE_FQ_NAME: FqName = FqName("std.ast")

/**
 * `synchronized` 表达式所需的标准库包。
 *
 * 官方 `SYNC_PACKAGE_NAME`（`include/cangjie/Utils/ConstantsUtils.h:86`）为 `std.sync`；
 * 诊断文本参数另用 `"sync"`（`SynchronizedExpr.cpp:27`），二者只在导入判据上一致。
 */
val SYNC_PACKAGE_FQ_NAME: FqName = FqName("std.sync")

/**
 * 判断该文件是否导入了 [packageFqName] 包本身或它的成员。
 *
 * 官方对 `quote` / `synchronized` 这类内建表达式形式用
 * `ImportManager::GetImportedDecl(package, name)` 判断所需标准库声明是否可用
 * （`SynchronizedExpr.cpp:23`、`QuoteExpr.cpp:20`）。该查询走 `CjoManager` 的包成员表，
 * 而包成员只在包已被导入并装载后才存在，因此等价判据是「该文件导入了这个包」。
 * `import a.b.*` 与 `import a.b.Member` 都以 [CfirImport.importedFqName] 表达，
 * 前者等于包名，后者以包名为父限定名。
 */
fun CfirFile.importsPackageOrMember(packageFqName: FqName): Boolean =
    imports.any { importDirective ->
        val importedFqName = importDirective.importedFqName ?: return@any false
        importedFqName == packageFqName || importedFqName.parent() == packageFqName
    }
