// 健康档案
export const GetHealthArchiveListUrl = '/health/archive/page'; // 健康档案列表
export const GetHealthArchiveUrl = '/health/archive/get'; // 健康档案详情
export const AddHealthArchiveUrl = '/health/archive/add';
export const UpdateHealthArchiveUrl = '/health/archive/update';
export const DeleteHealthArchiveUrl = '/health/archive/delete';
export const MatchHealthArchiveCustomerUrl = '/health/archive/match-customer';
export const SyncHealthArchiveUrl = '/health/archive/sync';

// 体检异常统计
export const GetHealthExamAbnormalStatUrl = '/health/examination/abnormal/stat';
export const GetHealthExamAbnormalCustomersUrl = '/health/examination/abnormal/customers';

// 体检数据同步
export const HealthSyncUrl = '/health/sync/sync'; // 触发体检数据同步（query: startDate, endDate）
export const HealthSyncDayUrl = '/health/sync/sync-day'; // 同步单日（query: date）
export const GetHealthSyncStatusUrl = '/health/sync/status'; // 获取同步状态

// 健康档案过敏史/病史/疫苗接种
export const GetHealthAllergyUrl = '/health/archive/allergy'; // 获取过敏史列表 GET /{archiveId}
export const SaveHealthAllergyUrl = '/health/archive/allergy/save'; // 保存过敏史 POST
export const DeleteHealthAllergyUrl = '/health/archive/allergy/delete'; // 删除过敏史 POST
export const GetHealthHistoryUrl = '/health/archive/history'; // 获取病史列表 GET /{archiveId}
export const SaveHealthHistoryUrl = '/health/archive/history/save'; // 保存病史 POST
export const DeleteHealthHistoryUrl = '/health/archive/history/delete'; // 删除病史 POST
export const GetHealthVaccinationUrl = '/health/archive/vaccination'; // 获取疫苗接种 GET /{archiveId}
export const SaveHealthVaccinationUrl = '/health/archive/vaccination/save'; // 保存疫苗接种 POST
export const DeleteHealthVaccinationUrl = '/health/archive/vaccination/delete'; // 删除疫苗接种 POST
export const GetHealthExaminationsUrl = '/health/archive/examination'; // 获取体检记录 GET /{archiveId}
export const SaveHealthExaminationUrl = '/health/archive/examination/save'; // 保存体检记录 POST
export const DeleteHealthExaminationUrl = '/health/archive/examination/delete'; // 删除体检记录 POST

// 随访记录
export const GetExamCustomerPageUrl = '/health/examination/page'; // 体检客户分页列表
export const GetHealthFollowListUrl = '/health/follow/page'; // 随访记录列表
export const GetHealthFollowUrl = '/health/follow/get'; // 随访记录详情
export const SaveHealthFollowUrl = '/health/follow/save'; // 新建/更新随访（合并）
export const DeleteHealthFollowUrl = '/health/follow/delete'; // 删除随访（path: /delete/{id}）
export const MarkHealthFollowContactedUrl = '/health/follow/mark-contacted'; // 标记已联系（path: /mark-contacted/{archiveId}）
export const RecordPhoneContactUrl = '/health/follow/record-phone-contact';
export const RecordSmsPushUrl = '/health/follow/record-sms-push';
export const BatchActionStatusUrl = '/health/follow/batch-action-status';

// 健康知识库
export const GetHealthKnowledgeListUrl = '/health/knowledge/page'; // 健康知识列表
export const GetHealthKnowledgeUrl = '/health/knowledge/get'; // 健康知识详情
export const GetHealthKnowledgeCategoriesUrl = '/health/knowledge/categories'; // 获取所有分类
export const SaveHealthKnowledgeUrl = '/health/knowledge/save'; // 新建/更新知识（合并）
export const DeleteHealthKnowledgeUrl = '/health/knowledge/delete'; // 删除知识（path: /delete/{id}）

// AI健康解读
export const HealthAiInterpretUrl = '/health/ai/interpret';
export const HealthAiEligibleArchivesUrl = '/health/ai/eligible-archives';
export const HealthAiLastInterpretationUrl = '/health/ai/last-interpretation';
export const HealthAiInterpretationHistoryUrl = '/health/ai/interpretation-history';
export const HealthAiSaveInterpretRecordUrl = '/health/ai/save-interpret-record';
export const HealthAiBatchInterpretationStatusUrl = '/health/ai/batch-interpretation-status';
export const HealthAiAssessRiskUrl = '/health/ai/assess-risk';
export const HealthAiDeleteInterpretRecordUrl = '/health/ai/delete-interpret-record';
export const HealthAiSummarizeSmsUrl = '/health/ai/summarize-sms';
export const HealthAiSendInviteSmsUrl = '/health/ai/send-invite-sms';
export const HealthKnowledgeSearchUrl = '/health/knowledge/search';

// 健康推送
export const HealthPushSendUrl = '/health/push/send';
export const HealthPushPageUrl = '/health/push/page';
export const HealthPushCustomerUrl = '/health/push/customer';
export const HealthPushKnowledgeUrl = '/health/push/knowledge';

// 随访规则
export const GetFollowRulePageUrl = '/health/rule/page';
export const GetFollowRuleUrl = '/health/rule/get';
export const SaveFollowRuleUrl = '/health/rule/save';
export const DeleteFollowRuleUrl = '/health/rule/delete';
export const EnableFollowRuleUrl = '/health/rule/enable';
export const DisableFollowRuleUrl = '/health/rule/disable';
export const EvaluateFollowRulesUrl = '/health/rule/evaluate';
