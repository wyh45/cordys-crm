import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  AddHealthArchiveUrl,
  BatchActionStatusUrl,
  DeleteFollowRuleUrl,
  DeleteHealthAllergyUrl,
  DeleteHealthArchiveUrl,
  DeleteHealthExaminationUrl,
  DeleteHealthFollowUrl,
  DeleteHealthHistoryUrl,
  DeleteHealthKnowledgeUrl,
  DeleteHealthVaccinationUrl,
  DisableFollowRuleUrl,
  EnableFollowRuleUrl,
  EvaluateFollowRulesUrl,
  GetFollowRulePageUrl,
  GetFollowRuleUrl,
  GetHealthAllergyUrl,
  GetHealthArchiveListUrl,
  GetHealthArchiveUrl,
  GetHealthExamAbnormalCustomersUrl,
  GetHealthExamAbnormalStatUrl,
  GetHealthExaminationsUrl,
  GetHealthFollowListUrl,
  GetHealthFollowUrl,
  GetHealthHistoryUrl,
  GetHealthKnowledgeCategoriesUrl,
  GetHealthKnowledgeListUrl,
  GetHealthKnowledgeUrl,
  GetHealthSyncStatusUrl,
  GetHealthVaccinationUrl,
  HealthAiAssessRiskUrl,
  HealthAiBatchInterpretationStatusUrl,
  HealthAiDeleteInterpretRecordUrl,
  HealthAiEligibleArchivesUrl,
  HealthAiInterpretationHistoryUrl,
  HealthAiInterpretUrl,
  HealthAiLastInterpretationUrl,
  HealthAiSaveInterpretRecordUrl,
  HealthAiSendInviteSmsUrl,
  HealthAiSummarizeSmsUrl,
  HealthKnowledgeSearchUrl,
  HealthPushCustomerUrl,
  HealthPushKnowledgeUrl,
  HealthPushPageUrl,
  HealthPushSendUrl,
  HealthSyncDayUrl,
  HealthSyncUrl,
  MarkHealthFollowContactedUrl,
  MatchHealthArchiveCustomerUrl,
  RecordPhoneContactUrl,
  RecordSmsPushUrl,
  SaveFollowRuleUrl,
  SaveHealthAllergyUrl,
  SaveHealthExaminationUrl,
  SaveHealthFollowUrl,
  SaveHealthHistoryUrl,
  SaveHealthKnowledgeUrl,
  SaveHealthVaccinationUrl,
  SyncHealthArchiveUrl,
  UpdateHealthArchiveUrl,
} from '@lib/shared/api/requrls/health';
import type { CommonList } from '@lib/shared/models/common';

// 健康档案相关类型
export interface HealthArchive {
  id: string;
  customerId: string;
  customerName?: string;
  archiveNo?: string;
  bloodType?: string;
  allergies?: string;
  pastMedicalHistory?: string;
  familyHistory?: string;
  height?: number;
  weight?: number;
  bloodPressure?: string;
  heartRate?: number;
  phone?: string;
  gender?: string;
  age?: number;
  idcardNo?: string;
  smoking?: string;
  drinking?: string;
  sleepQuality?: string;
  exercise?: string;
  diet?: string;
  bloodSugar?: number;
  createTime?: string;
  updateTime?: string;
}

export interface HealthArchiveListItem extends HealthArchive {
  // 列表展示所需的额外字段
}

export interface HealthArchiveParams {
  page?: number;
  pageSize?: number;
  customerId?: string;
  keyword?: string;
}

// 体检同步相关类型
export interface HealthSyncParams {
  startDate: string;
  endDate: string;
}

export interface HealthSyncDayParams {
  date: string;
}

export interface HealthSyncResult {
  syncId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  message?: string;
}

// 健康档案过敏史/病史/疫苗接种类型
export interface HealthAllergy {
  id?: string;
  customerId?: string;
  allergen: string;
  severity?: string; // MILD/MODERATE/SEVERE
}

export interface HealthMedicalHistory {
  id?: string;
  customerId?: string;
  diseaseName: string;
  diagnosisDate?: string;
  treatmentStatus?: string; // TREATMENT/PROGRESS/CURED
  remarks?: string;
}

export interface HealthVaccination {
  id?: string;
  customerId?: string;
  vaccineName: string;
  vaccinateDate?: string;
  nextDoseDate?: string;
}

export interface HealthExamination {
  id: string;
  customerId?: string;
  examNo?: string;
  examDate?: number;
  examItem?: string;
  resultValue?: string;
  referenceRange?: string;
  isAbnormal?: boolean;
  resultFlag?: string;
  createTime?: string;
}

// 体检异常统计相关类型
export interface ExamItemStat {
  examItem: string;
  totalCount: number;
  abnormalCount: number;
  abnormalRate: number;
  level: string;
}

export interface CustomerAbnormalStat {
  customerId: string;
  customerName?: string;
  phone?: string;
  archiveId?: string;
  totalItems: number;
  abnormalItems: number;
  abnormalRate: number;
}

export interface HealthExamAbnormalStat {
  byItem: ExamItemStat[];
  byCustomer: CustomerAbnormalStat[];
  totalExamRecords: number;
  abnormalExamRecords: number;
  abnormalRate: number;
}

export interface HealthExamAbnormalParams {
  startDate?: number;
  endDate?: number;
  minRecords?: number;
}

// 随访记录相关类型
export interface HealthFollow {
  id: string;
  customerId: string;
  customerName?: string;
  followUpDate: string;
  followUpType: string;
  symptoms?: string;
  diagnosis?: string;
  prescription?: string;
  nextFollowUpDate?: string;
  remarks?: string;
  createTime?: string;
  updateTime?: string;
}

export interface HealthFollowListItem extends HealthFollow {
  // 列表展示所需的额外字段
}

export interface HealthFollowParams {
  page?: number;
  pageSize?: number;
  customerId?: string;
  keyword?: string;
  startDate?: string;
  endDate?: string;
}

// 健康知识库相关类型
export interface HealthKnowledge {
  id: string;
  title: string;
  category: string;
  tags?: string[];
  content: string;
  author?: string;
  publishDate?: string;
  views?: number;
  createTime?: string;
  updateTime?: string;
}

export interface HealthKnowledgeListItem extends HealthKnowledge {
  // 列表展示所需的额外字段
}

export interface HealthKnowledgeParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  category?: string;
}

// AI解读相关类型
export interface HealthAiInterpretParams {
  archiveId: string;
  reportType: 'BLOOD_TEST' | 'URINE_TEST' | 'ECG' | 'X_RAY' | 'CT' | 'MRI' | 'GENERAL';
  reportData: Record<string, any>;
  select: string[];
}

export interface HealthAiInterpretResult {
  results: Record<string, string>;
  patientPhone?: string;
}

export interface HealthAiInterpretRecord {
  id: string;
  customerId: string;
  archiveId: string;
  customerName: string;
  suggestionType?: string;
  interpretation: string;
  pushContent?: string;
  pushChannel?: string;
  interpretTime: number;
  createTime?: number;
}

// 健康推送相关类型
export interface HealthPushRequest {
  customerIds?: string[];
  knowledgeId?: string;
  channel: 'SMS' | 'EMAIL' | 'INSITE' | 'DINGTALK' | 'LARK' | 'WECOM';
  title?: string;
  content?: string;
}

export interface HealthPushRecord {
  id: string;
  customerId: string;
  knowledgeId: string;
  pushChannel: string;
  pushStatus: string;
  pushTime: number;
  createTime?: number;
  updateTime?: number;
}

export interface HealthPushListItem extends HealthPushRecord {}

// 随访规则类型
export interface HealthFollowRule {
  id: string | null;
  name: string;
  description?: string;
  conditionType?: string;
  conditionExpr?: string;
  followType?: string;
  followResultTemplate?: string;
  followIntervalDays?: number;
  enabled: boolean;
  priority?: number;
  watchExamItems?: string;
  minAbnormalCount?: number;
  followMethod?: string;
  followInterval?: number;
}

export interface HealthFollowRuleParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// 异常客户明细
export interface CustomerAbnormalDetail {
  customerId: string;
  archiveId?: string;
  archiveNo?: string;
  customerName?: string;
  phone?: string;
  abnormalItems: number;
  abnormalItemNames?: string[];
  synced?: boolean;
  syncedCount?: number;
}

// 规则匹配的档案
export interface RuleMatchedArchive {
  ruleId: string;
  ruleName: string;
  followMethod: string;
  followInterval: number;
  archiveId: string;
  archiveNo?: string;
  customerName?: string;
  phone?: string;
  abnormalCount: number;
  matchedExamItems: string[];
  synced?: boolean;
}

// AI可选档案
export interface HealthAiEligibleArchive {
  id: string;
  archiveNo?: string;
  customerName?: string;
  phone?: string;
  gender?: string;
  age?: number;
  customerId?: string;
  source: 'abnormal_exam' | 'follow_rule' | 'both';
}

export default function useHealthApi(CDR: CordysAxios) {
  // ============ 健康档案 ============

  // 获取健康档案列表
  function getHealthArchiveList(data: HealthArchiveParams) {
    return CDR.post<CommonList<HealthArchiveListItem>>({ url: GetHealthArchiveListUrl, data });
  }

  // 获取健康档案详情
  function getHealthArchive(id: string) {
    return CDR.get<HealthArchive>({ url: `${GetHealthArchiveUrl}/${id}` });
  }

  // 添加健康档案
  function addHealthArchive(data: Partial<HealthArchive>) {
    return CDR.post({ url: AddHealthArchiveUrl, data });
  }

  // 更新健康档案
  function updateHealthArchive(data: Partial<HealthArchive>) {
    return CDR.post({ url: UpdateHealthArchiveUrl, data });
  }

  // 删除健康档案
  function deleteHealthArchive(id: string) {
    return CDR.post({ url: DeleteHealthArchiveUrl, data: { id } });
  }

  function matchHealthArchiveCustomer(
    id: string
  ): Promise<{ matched: boolean; customerId?: string; customerName?: string; reason?: string }> {
    return CDR.post({ url: MatchHealthArchiveCustomerUrl, data: { id } });
  }

  function syncHealthArchive(
    id: string
  ): Promise<{ success: boolean; customerName: string; archiveId: string; createdCount: number; reason?: string }> {
    return CDR.post({ url: SyncHealthArchiveUrl, data: { id } });
  }

  // ============ 体检数据同步 ============

  // 触发体检数据同步（按日期范围，query param）
  function triggerHealthSync(data: HealthSyncParams) {
    return CDR.post<HealthSyncResult>({ url: HealthSyncUrl, params: data });
  }

  // 同步单日
  function triggerHealthSyncDay(data: HealthSyncDayParams) {
    return CDR.post<HealthSyncResult>({ url: HealthSyncDayUrl, params: data });
  }

  // 获取同步状态
  function getHealthSyncStatus(syncId: string) {
    return CDR.get<HealthSyncResult>({ url: `${GetHealthSyncStatusUrl}/${syncId}` });
  }

  // ============ 健康档案子表（过敏史/病史/疫苗接种/体检记录） ============

  // 获取过敏史列表
  function getHealthAllergies(archiveId: string) {
    return CDR.get<HealthAllergy[]>({ url: `${GetHealthAllergyUrl}/${archiveId}` });
  }

  // 保存过敏史
  function saveHealthAllergy(data: Partial<HealthAllergy>) {
    return CDR.post({ url: SaveHealthAllergyUrl, data });
  }

  // 删除过敏史
  function deleteHealthAllergy(id: string) {
    return CDR.post({ url: DeleteHealthAllergyUrl, data: { id } });
  }

  // 获取病史列表
  function getHealthHistories(archiveId: string) {
    return CDR.get<HealthMedicalHistory[]>({ url: `${GetHealthHistoryUrl}/${archiveId}` });
  }

  // 保存病史
  function saveHealthHistory(data: Partial<HealthMedicalHistory>) {
    return CDR.post({ url: SaveHealthHistoryUrl, data });
  }

  // 删除病史
  function deleteHealthHistory(id: string) {
    return CDR.post({ url: DeleteHealthHistoryUrl, data: { id } });
  }

  // 获取疫苗接种列表
  function getHealthVaccinations(archiveId: string) {
    return CDR.get<HealthVaccination[]>({ url: `${GetHealthVaccinationUrl}/${archiveId}` });
  }

  // 保存疫苗接种
  function saveHealthVaccination(data: Partial<HealthVaccination>) {
    return CDR.post({ url: SaveHealthVaccinationUrl, data });
  }

  // 删除疫苗接种
  function deleteHealthVaccination(id: string) {
    return CDR.post({ url: DeleteHealthVaccinationUrl, data: { id } });
  }

  // 获取体检记录列表
  function getHealthExaminations(archiveId: string) {
    return CDR.get<HealthExamination[]>({ url: `${GetHealthExaminationsUrl}/${archiveId}` });
  }

  // 保存体检记录
  function saveHealthExamination(data: Partial<HealthExamination>) {
    return CDR.post({ url: SaveHealthExaminationUrl, data });
  }

  // 删除体检记录
  function deleteHealthExamination(id: string) {
    return CDR.post({ url: DeleteHealthExaminationUrl, data: { id } });
  }

  // 获取体检异常统计
  function getHealthExamAbnormalStat(data: HealthExamAbnormalParams) {
    return CDR.get<HealthExamAbnormalStat>({ url: GetHealthExamAbnormalStatUrl, params: data });
  }

  // 获取异常客户列表
  function getHealthExamAbnormalCustomers(params?: { startDate?: number; endDate?: number }) {
    return CDR.get<CustomerAbnormalDetail[]>({ url: GetHealthExamAbnormalCustomersUrl, params });
  }

  // ============ 随访记录 ============

  // 获取随访记录列表
  function getHealthFollowList(data: HealthFollowParams) {
    return CDR.post<CommonList<HealthFollowListItem>>({ url: GetHealthFollowListUrl, data });
  }

  // 获取随访记录详情
  function getHealthFollow(id: string) {
    return CDR.get<HealthFollow>({ url: `${GetHealthFollowUrl}/${id}` });
  }

  // 添加/更新随访记录（合并为 save）
  function saveHealthFollow(data: Partial<HealthFollow>) {
    return CDR.post({ url: SaveHealthFollowUrl, data });
  }

  // 删除随访记录（path param）
  function deleteHealthFollow(id: string) {
    return CDR.post({ url: `${DeleteHealthFollowUrl}/${id}` });
  }

  // 标记已联系
  function markHealthFollowContacted(archiveId: string) {
    return CDR.post({ url: `${MarkHealthFollowContactedUrl}/${archiveId}` });
  }

  // 记录电话随访
  function recordPhoneContact(archiveId: string, followInterval: number) {
    return CDR.post({ url: `${RecordPhoneContactUrl}/${archiveId}`, data: { followInterval } });
  }

  // 记录短信推送
  function recordSmsPush(archiveId: string, followInterval: number) {
    return CDR.post({ url: `${RecordSmsPushUrl}/${archiveId}`, data: { followInterval } });
  }

  // 批量查询随访操作状态
  function batchCheckActionStatus(archiveIds: string[]) {
    return CDR.post<Record<string, any>>({ url: BatchActionStatusUrl, data: { archiveIds } });
  }

  // ============ 健康知识库 ============

  // 获取健康知识列表
  function getHealthKnowledgeList(data: HealthKnowledgeParams) {
    return CDR.post<CommonList<HealthKnowledgeListItem>>({ url: GetHealthKnowledgeListUrl, data });
  }

  // 获取健康知识详情
  function getHealthKnowledge(id: string) {
    return CDR.get<HealthKnowledge>({ url: `${GetHealthKnowledgeUrl}/${id}` });
  }

  // 添加/更新健康知识（合并为 save）
  function saveHealthKnowledge(data: Partial<HealthKnowledge>) {
    return CDR.post({ url: SaveHealthKnowledgeUrl, data });
  }

  // 删除健康知识（path param）
  function deleteHealthKnowledge(id: string) {
    return CDR.post({ url: `${DeleteHealthKnowledgeUrl}/${id}` });
  }

  // 获取知识分类
  function searchHealthKnowledge(query: string, topK?: number) {
    return CDR.post<HealthKnowledgeListItem[]>({ url: HealthKnowledgeSearchUrl, data: { query, topK: topK || 10 } });
  }

  function getHealthKnowledgeCategories() {
    return CDR.get<string[]>({ url: GetHealthKnowledgeCategoriesUrl });
  }

  // ============ AI健康解读 ============

  // AI解读体检报告
  function healthAiInterpret(data: HealthAiInterpretParams) {
    return CDR.post<HealthAiInterpretResult>({ url: HealthAiInterpretUrl, data });
  }

  // 获取可AI解读的档案列表
  function getHealthAiEligibleArchives(data?: { page?: number; pageSize?: number }) {
    return CDR.post<{ list: HealthAiEligibleArchive[]; total: number }>({ url: HealthAiEligibleArchivesUrl, data });
  }

  // 获取最近一次AI解读记录
  function getLastInterpretation(archiveId: string) {
    return CDR.get<HealthAiInterpretRecord>({ url: `${HealthAiLastInterpretationUrl}/${archiveId}` });
  }

  // 获取AI解读历史列表
  function getInterpretationHistory(data: { page?: number; pageSize?: number }) {
    return CDR.post<HealthAiInterpretRecord[]>({ url: HealthAiInterpretationHistoryUrl, data });
  }

  // 保存AI解读+推送记录
  function saveInterpretRecord(data: {
    archiveId: string;
    customerId?: string;
    customerName?: string;
    suggestionType?: string;
    interpretation: string;
    pushContent?: string;
    pushChannel?: string;
  }) {
    return CDR.post({ url: HealthAiSaveInterpretRecordUrl, data });
  }

  function assessRisk(data: { archiveId: string; reportType: string; reportData: Record<string, any> }) {
    return CDR.post<{ result: string }>({ url: HealthAiAssessRiskUrl, data });
  }

  function deleteInterpretRecord(id: string) {
    return CDR.post<void>({ url: `${HealthAiDeleteInterpretRecordUrl}/${id}` });
  }

  function batchInterpretationStatus(archiveIds: string[]) {
    return CDR.post<Record<string, string[]>>({ url: HealthAiBatchInterpretationStatusUrl, data: { archiveIds } });
  }

  function summarizeSms(data: { records: { type: string; content: string }[]; knowledge?: string; customerName?: string; gender?: string }) {
    return CDR.post<{ greeting: string; finding: string; action: string }>({ url: HealthAiSummarizeSmsUrl, data });
  }

  function sendInviteSms(data: { phone: string; greeting: string; finding: string; action: string; archiveId?: string; customerName?: string }) {
    return CDR.post<{ success: boolean; message: string }>({ url: HealthAiSendInviteSmsUrl, data });
  }

  // ============ 健康推送 ============

  // 推送健康知识
  function pushHealthKnowledge(data: HealthPushRequest) {
    return CDR.post({ url: HealthPushSendUrl, data });
  }

  // 获取推送记录（分页）
  function getHealthPushPage(params: { page?: number; pageSize?: number }) {
    return CDR.post<HealthPushListItem[]>({ url: HealthPushPageUrl, params });
  }

  // 按客户查推送记录
  function getHealthPushByCustomer(customerId: string) {
    return CDR.get<HealthPushListItem[]>({ url: `${HealthPushCustomerUrl}/${customerId}` });
  }

  // 按知识查推送记录
  function getHealthPushByKnowledge(knowledgeId: string) {
    return CDR.get<HealthPushListItem[]>({ url: `${HealthPushKnowledgeUrl}/${knowledgeId}` });
  }

  // ============ 随访规则 ============

  // 获取随访规则列表（分页）
  function getFollowRulePage(data: HealthFollowRuleParams) {
    return CDR.post<HealthFollowRule[]>({ url: GetFollowRulePageUrl, data });
  }

  // 获取随访规则详情
  function getFollowRule(id: string) {
    return CDR.get<HealthFollowRule>({ url: `${GetFollowRuleUrl}/${id}` });
  }

  // 保存随访规则（新建/更新）
  function saveFollowRule(data: Partial<HealthFollowRule>) {
    return CDR.post({ url: SaveFollowRuleUrl, data });
  }

  // 删除随访规则
  function deleteFollowRule(id: string) {
    return CDR.post({ url: `${DeleteFollowRuleUrl}/${id}` });
  }

  // 启用随访规则
  function enableFollowRule(id: string) {
    return CDR.post({ url: `${EnableFollowRuleUrl}/${id}` });
  }

  // 禁用随访规则
  function disableFollowRule(id: string) {
    return CDR.post({ url: `${DisableFollowRuleUrl}/${id}` });
  }

  // 评估所有规则，返回匹配的档案
  function evaluateFollowRules() {
    return CDR.get<RuleMatchedArchive[]>({ url: EvaluateFollowRulesUrl });
  }

  return {
    // 健康档案
    getHealthArchiveList,
    getHealthArchive,
    addHealthArchive,
    updateHealthArchive,
    deleteHealthArchive,
    matchHealthArchiveCustomer,
    syncHealthArchive,
    // 体检数据同步
    triggerHealthSync,
    triggerHealthSyncDay,
    getHealthSyncStatus,
    // 健康档案子表
    getHealthAllergies,
    saveHealthAllergy,
    deleteHealthAllergy,
    getHealthHistories,
    saveHealthHistory,
    deleteHealthHistory,
    getHealthVaccinations,
    saveHealthVaccination,
    deleteHealthVaccination,
    getHealthExaminations,
    saveHealthExamination,
    deleteHealthExamination,
    getHealthExamAbnormalStat,
    getHealthExamAbnormalCustomers,
    // 随访记录
    getHealthFollowList,
    getHealthFollow,
    saveHealthFollow,
    deleteHealthFollow,
    markHealthFollowContacted,
    recordPhoneContact,
    recordSmsPush,
    batchCheckActionStatus,
    // 健康知识库
    getHealthKnowledgeList,
    getHealthKnowledge,
    saveHealthKnowledge,
    deleteHealthKnowledge,
    getHealthKnowledgeCategories,
    searchHealthKnowledge,
    // AI健康解读
    healthAiInterpret,
    getHealthAiEligibleArchives,
    getLastInterpretation,
    getInterpretationHistory,
    saveInterpretRecord,
    batchInterpretationStatus,
    assessRisk,
    deleteInterpretRecord,
    summarizeSms,
    sendInviteSms,
    // 健康推送
    pushHealthKnowledge,
    getHealthPushPage,
    getHealthPushByCustomer,
    getHealthPushByKnowledge,
    // 随访规则
    getFollowRulePage,
    getFollowRule,
    saveFollowRule,
    deleteFollowRule,
    enableFollowRule,
    disableFollowRule,
    evaluateFollowRules,
  };
}
