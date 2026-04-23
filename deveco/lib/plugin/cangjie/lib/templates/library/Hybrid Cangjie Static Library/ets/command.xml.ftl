<#if action == "CreatePagePackage">
    <#if apiType == "stageMode">
        <#include  "commands/stagePageCommand.xml.ftl"/>
    <#else>
        <#include "commands/pageCommand.xml.ftl"/>
    </#if>
<#else>
    <copy src="ets/code template/gitignore_file" dest="${rootOut}/.gitignore"/>
    <merge src="ets/code template/project-build-profile.json5.ftl" dest="${projectPath}/build-profile.json5"/>
    <#if enableNative>
        <mkdir dest="${rootOut}/libs/arm64-v8a" />
    </#if>
    <#if apiType == "stageMode">
        <#include  "commands/stageCommand.xml.ftl"/>
    <#else>
        <#include  "commands/faCommand.xml.ftl"/>
    </#if>
</#if>
