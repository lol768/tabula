${extension.universityId} had an extension until ${dateOnlyFormatter.print(extension.expiryDate)} for the assignment ${assignment.module.code?upper_case} ${assignment.module.name} ${assignment.name}. Their feedback is due in <#if daysLeft == 0>today<#else>${daysLeft} working days on ${dateOnlyFormatter.print(deadline)}</#if>.