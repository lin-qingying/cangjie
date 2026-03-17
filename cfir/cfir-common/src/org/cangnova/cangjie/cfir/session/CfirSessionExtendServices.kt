package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService

val CfirSession.extendRuleQueryService: CfirExtendRuleQueryService by CfirSession.sessionComponentAccessor()

val CfirSession.extendRuleQueryServiceOrNull: CfirExtendRuleQueryService? by CfirSession.nullableSessionComponentAccessor()
