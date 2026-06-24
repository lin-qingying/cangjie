package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification

/**
 * session 上冻结的 macro demand classification。
 */
val CfirSession.macroDemandClassification: MacroDemandClassification by CfirSession.sessionComponentAccessor()

/**
 * session 上冻结的 macro demand classification，可空访问版本。
 */
val CfirSession.macroDemandClassificationOrNull: MacroDemandClassification? by CfirSession.nullableSessionComponentAccessor()
