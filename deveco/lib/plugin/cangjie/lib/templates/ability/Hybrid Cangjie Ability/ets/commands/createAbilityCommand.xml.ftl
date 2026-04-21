<#--  src  -->
<#if apiType == "stageMode">
    <#if (projectType?? && projectType == "Application") || (atomicService?? && !atomicService)>
        <copy src="../../common/media/startIcon.png" dest="${rootOut}/src/main/resources/base/media/startIcon.png"/>
        <merge src="ets/code template/src/stageMode/common/ets/module.json5.ftl" dest="${rootOut}/src/main/module.json5"/>
    <#else>
        <copy src="../../common/media/icon.png" dest="${rootOut}/src/main/resources/base/media/startIcon.png"/>
        <merge src="ets/code template/src/stageMode/common/ets/module.json5_automic.ftl" dest="${rootOut}/src/main/module.json5"/>
    </#if>
    <#if compatibleBaseApi?? && compatibleBaseApi gte 11 && apiType == "stageMode">
        <instantiate src="ets/code template/src/stageMode/traditionalCode/ets/MainAbility/MainAbility_kit.ets.ftl"
                    dest="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>
    <#else>
        <instantiate src="ets/code template/src/stageMode/traditionalCode/ets/MainAbility/MainAbility.ts.ftl"
                    dest="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>
    </#if>
    <#if hasIndex?? && !hasIndex>
        <copy src="ets/code template/src/stageMode/traditionalCode/ets/pages/Index.ets" dest="${rootOut}/src/main/ets/pages/Index.ets"/>
        <merge src="ets/code template/res/profile/main_pages.json" dest="${rootOut}/src/main/resources/base/profile/main_pages.json"/>
    </#if>
    
    <open file="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>
<#else >
    <#include "faCommand.xml.ftl"/>
</#if>

