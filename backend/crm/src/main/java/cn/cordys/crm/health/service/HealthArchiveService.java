package cn.cordys.crm.health.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.customer.domain.CustomerContact;
import cn.cordys.crm.health.domain.HealthAllergy;
import cn.cordys.crm.health.domain.HealthArchive;
import cn.cordys.crm.health.domain.HealthArchiveMapping;
import cn.cordys.crm.health.domain.HealthExamination;
import cn.cordys.crm.health.domain.HealthMedicalHistory;
import cn.cordys.crm.health.domain.HealthVaccination;
import cn.cordys.crm.health.dto.SyncResult;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康档案服务
 */
@Service
public class HealthArchiveService {

    @Resource
    private BaseMapper<HealthArchive> archiveMapper;

    @Resource
    private BaseMapper<HealthExamination> examMapper;

    @Resource
    private BaseMapper<HealthAllergy> allergyMapper;

    @Resource
    private BaseMapper<HealthMedicalHistory> historyMapper;

    @Resource
    private BaseMapper<HealthVaccination> vaccinationMapper;

    @Resource
    private BaseMapper<HealthArchiveMapping> mappingMapper;

    @Resource
    private BaseMapper<CustomerContact> contactMapper;

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Resource
    private HealthFollowRuleService followRuleService;

    public List<HealthArchive> listArchives(String keyword) {
        HealthArchive criteria = new HealthArchive();
        List<HealthArchive> list = archiveMapper.select(criteria);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.toLowerCase();
            list = list.stream()
                .filter(a ->
                    (a.getCustomerName() != null && a.getCustomerName().toLowerCase().contains(kw)) ||
                    (a.getPhone() != null && a.getPhone().contains(kw)) ||
                    (a.getIdcardNo() != null && a.getIdcardNo().contains(kw))
                )
                .toList();
        }
        return list;
    }

    public HealthArchive getArchive(String id) {
        HealthArchive archive = archiveMapper.selectByPrimaryKey(id);
        if (archive != null) {
            archive.setRiskScore(computeRiskScore(id));
        }
        return archive;
    }

    /**
     * 计算健康风险评分：基于异常检查项数量和偏离程度
     * 公式: min(100, abnormalCount * 20 + flagBonus)
     * flagBonus: ↑↓ 每个 +10
     */
    public int computeRiskScore(String archiveId) {
        HealthExamination criteria = new HealthExamination();
        criteria.setCustomerId(archiveId);
        List<HealthExamination> exams = examMapper.select(criteria);
        if (exams == null || exams.isEmpty()) return 0;

        int abnormalCount = 0;
        int flagBonus = 0;
        for (HealthExamination exam : exams) {
            if (Boolean.TRUE.equals(exam.getIsAbnormal())) {
                abnormalCount++;
            }
            String flag = exam.getResultFlag();
            if ("↑".equals(flag) || "↓".equals(flag)) {
                flagBonus += 10;
            }
        }
        return Math.min(100, abnormalCount * 20 + flagBonus);
    }

    public List<HealthExamination> getExaminations(String archiveId) {
        HealthExamination criteria = new HealthExamination();
        criteria.setCustomerId(archiveId);
        List<HealthExamination> list = examMapper.select(criteria);
        list.sort((a, b) -> Long.compare(
            b.getExamDate() != null ? b.getExamDate() : 0L,
            a.getExamDate() != null ? a.getExamDate() : 0L
        ));
        return list;
    }

    public void deleteAllergy(String id) {
        allergyMapper.deleteByPrimaryKey(id);
    }

    public void deleteHistory(String id) {
        historyMapper.deleteByPrimaryKey(id);
    }

    public void deleteVaccination(String id) {
        vaccinationMapper.deleteByPrimaryKey(id);
    }

    public void saveExamination(HealthExamination exam) {
        if (StringUtils.isBlank(exam.getId())) {
            exam.setId(IDGenerator.nextStr());
            exam.setCreateTime(System.currentTimeMillis());
        }
        if (StringUtils.isBlank(exam.getExamNo())) {
            exam.setExamNo("EX" + System.currentTimeMillis());
        }
        exam.setUpdateTime(System.currentTimeMillis());
        HealthExamination existing = examMapper.selectByPrimaryKey(exam.getId());
        if (existing != null && StringUtils.isNotBlank(existing.getId())) {
            examMapper.updateById(exam);
        } else {
            examMapper.insert(exam);
        }
    }

    public void deleteExamination(String id) {
        examMapper.deleteByPrimaryKey(id);
    }

    public void saveAllergy(HealthAllergy allergy) {
        if (StringUtils.isBlank(allergy.getId())) {
            allergy.setId(IDGenerator.nextStr());
            allergy.setCreateTime(System.currentTimeMillis());
        }
        allergy.setUpdateTime(System.currentTimeMillis());
        HealthAllergy existing = allergyMapper.selectByPrimaryKey(allergy.getId());
        if (existing != null && StringUtils.isNotBlank(existing.getId())) {
            allergyMapper.updateById(allergy);
        } else {
            allergyMapper.insert(allergy);
        }
    }

    public List<HealthAllergy> getAllergies(String archiveId) {
        HealthAllergy criteria = new HealthAllergy();
        criteria.setCustomerId(archiveId);
        return allergyMapper.select(criteria);
    }

    public void saveHistory(HealthMedicalHistory history) {
        if (StringUtils.isBlank(history.getId())) {
            history.setId(IDGenerator.nextStr());
            history.setCreateTime(System.currentTimeMillis());
        }
        history.setUpdateTime(System.currentTimeMillis());
        HealthMedicalHistory existing = historyMapper.selectByPrimaryKey(history.getId());
        if (existing != null && StringUtils.isNotBlank(existing.getId())) {
            historyMapper.updateById(history);
        } else {
            historyMapper.insert(history);
        }
    }

    public List<HealthMedicalHistory> getHistories(String archiveId) {
        HealthMedicalHistory criteria = new HealthMedicalHistory();
        criteria.setCustomerId(archiveId);
        return historyMapper.select(criteria);
    }

    public void saveVaccination(HealthVaccination vaccination) {
        if (StringUtils.isBlank(vaccination.getId())) {
            vaccination.setId(IDGenerator.nextStr());
            vaccination.setCreateTime(System.currentTimeMillis());
        }
        vaccination.setUpdateTime(System.currentTimeMillis());
        HealthVaccination existing = vaccinationMapper.selectByPrimaryKey(vaccination.getId());
        if (existing != null && StringUtils.isNotBlank(existing.getId())) {
            vaccinationMapper.updateById(vaccination);
        } else {
            vaccinationMapper.insert(vaccination);
        }
    }

    public List<HealthVaccination> getVaccinations(String archiveId) {
        HealthVaccination criteria = new HealthVaccination();
        criteria.setCustomerId(archiveId);
        return vaccinationMapper.select(criteria);
    }

    public void addArchive(HealthArchive archive) {
        if (StringUtils.isBlank(archive.getId())) {
            archive.setId(IDGenerator.nextStr());
            archive.setCreateTime(System.currentTimeMillis());
        }
        archive.setUpdateTime(System.currentTimeMillis());
        archiveMapper.insert(archive);
        followRuleService.triggerRules(archive);
    }

    public void updateArchive(HealthArchive archive) {
        archive.setUpdateTime(System.currentTimeMillis());
        archiveMapper.updateById(archive);
        followRuleService.triggerRules(archive);
    }

    public void deleteArchive(String id) {
        archiveMapper.deleteByPrimaryKey(id);
    }

    /**
     * 检查档案是否已关联外部体检数据
     */
    public Map<String, Object> matchCustomer(String archiveId) {
        HealthArchive archive = archiveMapper.selectByPrimaryKey(archiveId);
        Map<String, Object> result = new LinkedHashMap<>();
        if (archive == null) {
            result.put("matched", false);
            result.put("reason", "档案不存在");
            return result;
        }

        HealthArchiveMapping mappingCriteria = new HealthArchiveMapping();
        mappingCriteria.setArchiveId(archiveId);
        List<HealthArchiveMapping> mappings = mappingMapper.select(mappingCriteria);
        if ((mappings == null || mappings.isEmpty()) && archive.getIdcardNo() != null && !archive.getIdcardNo().isBlank()) {
            HealthArchiveMapping idcardCriteria = new HealthArchiveMapping();
            idcardCriteria.setIdcardNo(archive.getIdcardNo());
            mappings = mappingMapper.select(idcardCriteria);
        }

        if (mappings == null || mappings.isEmpty()) {
            result.put("matched", false);
            result.put("reason", "该档案未关联外部体检数据，请先执行体检数据同步");
        } else {
            result.put("matched", true);
            result.put("customerId", archive.getId());
            result.put("customerName", archive.getCustomerName());
        }
        return result;
    }

    @Transactional
    public SyncResult syncToExamination(String archiveId) {
        HealthArchive archive = archiveMapper.selectByPrimaryKey(archiveId);
        if (archive == null) return null;

        SyncResult result = new SyncResult();
        result.setCustomerName(archive.getCustomerName());
        result.setArchiveId(archiveId);

        HealthArchiveMapping mappingCriteria = new HealthArchiveMapping();
        mappingCriteria.setArchiveId(archiveId);
        List<HealthArchiveMapping> mappings = mappingMapper.select(mappingCriteria);
        if ((mappings == null || mappings.isEmpty()) && archive.getIdcardNo() != null && !archive.getIdcardNo().isBlank()) {
            HealthArchiveMapping idcardCriteria = new HealthArchiveMapping();
            idcardCriteria.setIdcardNo(archive.getIdcardNo());
            mappings = mappingMapper.select(idcardCriteria);
        }

        if (mappings == null || mappings.isEmpty()) {
            result.setSuccess(false);
            result.setReason("该档案未关联外部体检数据，请先执行体检数据同步");
            return result;
        }

        String targetId = archive.getId();
        long now = System.currentTimeMillis();
        int createdCount = 0;

        if (syncField(targetId, "身高", archive.getHeight() != null ? archive.getHeight().toString() : null, "参考值: <200cm", now)) createdCount++;
        if (syncField(targetId, "体重", archive.getWeight() != null ? archive.getWeight().toString() : null, "参考值: <200kg", now)) createdCount++;
        if (syncField(targetId, "血压", archive.getBloodPressure(), "参考值: 90-140/60-90mmHg", now)) createdCount++;
        if (syncField(targetId, "心率", archive.getHeartRate() != null ? archive.getHeartRate().toString() : null, "参考值: 60-100次/分", now)) createdCount++;
        if (syncField(targetId, "血糖", archive.getBloodSugar() != null ? archive.getBloodSugar().toString() : null, "参考值: 3.9-6.1mmol/L", now)) createdCount++;
        if (syncField(targetId, "吸烟", archive.getSmoking(), null, now)) createdCount++;
        if (syncField(targetId, "饮酒", archive.getDrinking(), null, now)) createdCount++;
        if (syncField(targetId, "睡眠质量", archive.getSleepQuality(), null, now)) createdCount++;
        if (syncField(targetId, "运动频率", archive.getExercise(), null, now)) createdCount++;
        if (syncField(targetId, "饮食习惯", archive.getDiet(), null, now)) createdCount++;

        HealthAllergy allergyCriteria = new HealthAllergy();
        allergyCriteria.setCustomerId(archiveId);
        List<HealthAllergy> allergies = allergyMapper.select(allergyCriteria);
        if (allergies != null) {
            for (HealthAllergy a : allergies) {
                String label = "过敏史: " + (a.getAllergen() != null ? a.getAllergen() : "未知");
                String val = a.getAllergen() != null ? a.getAllergen() : "";
                if (a.getSeverity() != null) val += " [" + a.getSeverity() + "]";
                if (syncField(targetId, label, val, null, now)) createdCount++;
            }
        }

        HealthMedicalHistory historyCriteria = new HealthMedicalHistory();
        historyCriteria.setCustomerId(archiveId);
        List<HealthMedicalHistory> histories = historyMapper.select(historyCriteria);
        if (histories != null) {
            for (HealthMedicalHistory h : histories) {
                String label = "病史: " + (h.getDiseaseName() != null ? h.getDiseaseName() : "未知");
                String val = h.getDiseaseName() != null ? h.getDiseaseName() : "";
                if (h.getTreatmentStatus() != null) val += " [" + h.getTreatmentStatus() + "]";
                if (syncField(targetId, label, val, null, now)) createdCount++;
            }
        }

        HealthVaccination vaccCriteria = new HealthVaccination();
        vaccCriteria.setCustomerId(archiveId);
        List<HealthVaccination> vaccinations = vaccinationMapper.select(vaccCriteria);
        if (vaccinations != null) {
            for (HealthVaccination v : vaccinations) {
                String label = "疫苗接种: " + (v.getVaccineName() != null ? v.getVaccineName() : "未知");
                String val = v.getVaccineName() != null ? v.getVaccineName() : "";
                if (v.getVaccinateDate() != null) val += " (接种: " + v.getVaccinateDate() + ")";
                if (syncField(targetId, label, val, null, now)) createdCount++;
            }
        }

        result.setCreatedCount(createdCount);
        result.setSuccess(true);
        return result;
    }

    public Map<String, Integer> batchSyncedCount(List<String> archiveIds) {
        Map<String, Integer> result = new HashMap<>();
        if (archiveIds == null || archiveIds.isEmpty()) return result;
        for (String id : archiveIds) result.put(id, 0);
        List<HealthExamination> allExams = examMapper.select(new HealthExamination());
        if (allExams == null) return result;
        for (HealthExamination exam : allExams) {
            if (exam.getCustomerId() != null
                && archiveIds.contains(exam.getCustomerId())
                && exam.getExamNo() != null
                && exam.getExamNo().startsWith("SYNC")) {
                result.merge(exam.getCustomerId(), 1, Integer::sum);
            }
        }
        return result;
    }

    public Map<String, Object> debugCustomers() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Customer> allCustomers = customerMapper.select(new Customer());
        result.put("customerCount", allCustomers != null ? allCustomers.size() : 0);
        if (allCustomers != null && !allCustomers.isEmpty()) {
            Customer first = allCustomers.get(0);
            result.put("firstId", first.getId());
            result.put("firstName", first.getName());
        }
        CustomerContact cc = new CustomerContact();
        List<CustomerContact> allContacts = contactMapper.select(cc);
        result.put("contactCount", allContacts != null ? allContacts.size() : 0);
        if (allContacts != null && !allContacts.isEmpty()) {
            result.put("firstContactPhone", allContacts.get(0).getPhone());
            result.put("firstContactCustomerId", allContacts.get(0).getCustomerId());
        }
        return result;
    }

    private boolean syncField(String customerId, String examItem, String value, String referenceRange, long examDate) {
        if (value == null || value.isBlank()) return false;

        HealthExamination criteria = new HealthExamination();
        criteria.setCustomerId(customerId);
        criteria.setExamItem(examItem);
        List<HealthExamination> existing = examMapper.select(criteria);
        if (existing != null && existing.stream().anyMatch(e -> examItem.equals(e.getExamItem()))) return false;

        HealthExamination exam = new HealthExamination();
        exam.setId(IDGenerator.nextStr());
        exam.setCustomerId(customerId);
        exam.setExamItem(examItem);
        exam.setResultValue(value);
        exam.setReferenceRange(referenceRange);
        exam.setExamDate(examDate);
        exam.setExamNo("SYNC" + System.currentTimeMillis());
        exam.setCreateTime(System.currentTimeMillis());
        exam.setUpdateTime(System.currentTimeMillis());
        examMapper.insert(exam);
        return true;
    }
}
