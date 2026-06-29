package org.cangnova.cangjie.fileClasses

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind

/**
 * 保存 `CjFile.cangjieFileFacadeFqName`，供PSI 模块流程读取节点结构或语义信息。
 */
val CjFile.cangjieFileFacadeFqName: FqName
    get() = when (val kind = stub?.kind) {
        is CangJieFileStubKind.WithPackage.Facade -> kind.facadeFqName
        else -> packageFqName
    }
