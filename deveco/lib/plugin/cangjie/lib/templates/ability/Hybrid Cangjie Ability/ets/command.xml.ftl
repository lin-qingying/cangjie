<#include "commands/stringCommand.xml.ftl"/>
<merge src="ets/code template/res/element/color.json.ftl" dest="${rootOut}/src/main/resources/base/element/color.json"/>
<#if apiType == "stageMode" && ((projectType?? && projectType == "Application") || (atomicService?? && !atomicService))>
    <copy src="../../common/media/background.png" dest="${rootOut}/src/main/resources/base/media/background.png"/>
    <copy src="../../common/media/foreground.png" dest="${rootOut}/src/main/resources/base/media/foreground.png"/>
    <copy src="../../common/media/layered_image.json" dest="${rootOut}/src/main/resources/base/media/layered_image.json"/>
<#elseif (projectType?? && projectType == "Application") || (atomicService?? && !atomicService)>
    <copy src="../../common/media/icon.png" dest="${rootOut}/src/main/resources/base/media/icon.png"/>
<#else>
    <copy src="../../common/media/icon.png" dest="${rootOut}/src/main/resources/base/media/icon.png"/>
</#if>
<#if action == "ohosCreateAbility">
<#-- 创建ability   -->
    <#include "commands/createAbilityCommand.xml.ftl" />
<#else>
<#-- 创建模块/工程  -->
    <#--  res  -->
    <mkdir dest="${rootOut}/src/main/resources/rawfile"/>
    <#if apiType == "stageMode">
        <#include "commands/stageCommand.xml.ftl"/>
    <#else>
        <#include "commands/faCommand.xml.ftl"/>
    </#if>
</#if>
