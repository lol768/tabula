Feedback for ${assignment.module.code?upper_case} ${assignment.module.name} ${assignment.name} is due <#if dueToday>today<#else>in <@fmt.p daysLeft "working day" /> on ${dateOnlyFormatter.print(deadline)}</#if>.