package org.cangnova.cangjie.fileClasses

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind

val CjFile.cangjieFileFacadeFqName: FqName
    get() = when (val kind = stub?.kind) {
        is CangJieFileStubKind.WithPackage.Facade -> kind.facadeFqName
        else -> packageFqName
    }
