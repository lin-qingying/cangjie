package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.CfirSourceGraphPublicationOwner

/** Position 的编号域与 FullId 分离：pkg0/file一基，外包pkg一基/file零基。 */
internal data class CjoSavedFileReference(val packageIndex: UInt, val fileIndex: UInt)

/** 原 context 在实际 scope 准备中发布的 FILE 来源，坐标合法性不影响这份绑定。 */
internal sealed interface CjoPositionFileBinding {
    val originalSource: CjoLoadedPackage
    val originalOwner: CfirSourceGraphPublicationOwner
    val originalBinder: Any
    val originalDomain: CfirSession
    val reference: CjoSavedFileReference

    class NoFile internal constructor(
        override val originalSource: CjoLoadedPackage,
        override val originalOwner: CfirSourceGraphPublicationOwner,
        override val originalBinder: Any,
        override val originalDomain: CfirSession,
        override val reference: CjoSavedFileReference,
    ) : CjoPositionFileBinding {
        init { require(reference.packageIndex == 0u && reference.fileIndex == 0u) }
    }

    class Loaded internal constructor(
        override val originalSource: CjoLoadedPackage,
        override val originalOwner: CfirSourceGraphPublicationOwner,
        override val originalBinder: Any,
        override val originalDomain: CfirSession,
        override val reference: CjoSavedFileReference,
        val file: CjoSnapshotTableEntry,
    ) : CjoPositionFileBinding {
        init { require(file.domain == CjoSnapshotTableDomain.FILE) }
        /** 由原 owner 的实际依赖选择产生；文件名、包名或 hash 相同不产生此资格。 */
        val belongsToOriginalContext: Boolean get() = file.snapshot === originalSource
    }
}
