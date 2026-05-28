package cn.cordys.crm.health.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.health.domain.HealthFollowRecord;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class HealthFollowService {

    @Resource
    private BaseMapper<HealthFollowRecord> followMapper;

    public Map<String, Object> listFollowRecords(int pageNum, int pageSize) {
        HealthFollowRecord criteria = new HealthFollowRecord();
        List<HealthFollowRecord> all = followMapper.select(criteria);
        all.sort((a, b) -> Long.compare(
            b.getFollowDate() != null ? b.getFollowDate() : 0L,
            a.getFollowDate() != null ? a.getFollowDate() : 0L
        ));
        int total = all.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<HealthFollowRecord> list = start >= total ? List.of() : all.subList(start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public HealthFollowRecord getFollowRecord(String id) {
        return followMapper.selectByPrimaryKey(id);
    }

    @Transactional
    public void saveFollowRecord(HealthFollowRecord record) {
        if (record.getId() == null) {
            record.setId(IDGenerator.nextStr());
            record.setCreateTime(System.currentTimeMillis());
        }
        record.setUpdateTime(System.currentTimeMillis());
        HealthFollowRecord existing = followMapper.selectByPrimaryKey(record.getId());
        if (existing != null && existing.getId() != null) {
            followMapper.updateById(record);
        } else {
            followMapper.insert(record);
        }
    }

    @Transactional
    public void deleteFollowRecord(String id) {
        followMapper.deleteByPrimaryKey(id);
    }

    @Transactional
    public void markContacted(String archiveId) {
        HealthFollowRecord record = new HealthFollowRecord();
        record.setId(IDGenerator.nextStr());
        record.setCustomerId(archiveId);
        record.setFollowDate(System.currentTimeMillis());
        record.setFollowType("电话");
        record.setFollowResult("客户已联系");
        record.setCreateUser("system");
        record.setCreateTime(System.currentTimeMillis());
        record.setUpdateTime(System.currentTimeMillis());
        followMapper.insert(record);
    }

    public List<HealthFollowRecord> getByArchiveId(String archiveId) {
        HealthFollowRecord criteria = new HealthFollowRecord();
        criteria.setCustomerId(archiveId);
        List<HealthFollowRecord> list = followMapper.select(criteria);
        list.sort((a, b) -> Long.compare(
            b.getFollowDate() != null ? b.getFollowDate() : 0L,
            a.getFollowDate() != null ? a.getFollowDate() : 0L
        ));
        return list;
    }

    @Transactional
    public void recordPhoneContact(String archiveId, int followIntervalDays) {
        HealthFollowRecord record = new HealthFollowRecord();
        record.setId(IDGenerator.nextStr());
        record.setCustomerId(archiveId);
        record.setFollowDate(System.currentTimeMillis());
        record.setFollowType("PHONE_FOLLOW");
        record.setFollowResult("已联系");
        long intervalMs = (long) followIntervalDays * 24 * 60 * 60 * 1000;
        record.setNextFollowDate(System.currentTimeMillis() + intervalMs);
        record.setCreateUser("system");
        record.setCreateTime(System.currentTimeMillis());
        record.setUpdateTime(System.currentTimeMillis());
        followMapper.insert(record);
    }

    @Transactional
    public void recordSmsPush(String archiveId, int followIntervalDays) {
        HealthFollowRecord record = new HealthFollowRecord();
        record.setId(IDGenerator.nextStr());
        record.setCustomerId(archiveId);
        record.setFollowDate(System.currentTimeMillis());
        record.setFollowType("SMS_FOLLOW");
        record.setFollowResult("AI推送成功");
        long intervalMs = (long) followIntervalDays * 24 * 60 * 60 * 1000;
        record.setNextFollowDate(System.currentTimeMillis() + intervalMs);
        record.setCreateUser("system");
        record.setCreateTime(System.currentTimeMillis());
        record.setUpdateTime(System.currentTimeMillis());
        followMapper.insert(record);
    }

    public Map<String, Map<String, Object>> batchCheckActionStatus(List<String> archiveIds) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        if (archiveIds == null || archiveIds.isEmpty()) return result;

        List<HealthFollowRecord> all = followMapper.select(new HealthFollowRecord());
        if (all == null) return result;

        long now = System.currentTimeMillis();
        for (String aid : archiveIds) {
            Map<String, Object> status = new HashMap<>();
            HealthFollowRecord latestSms = all.stream()
                .filter(r -> aid.equals(r.getCustomerId()) && "SMS_FOLLOW".equals(r.getFollowType()))
                .max(Comparator.comparingLong(r -> r.getFollowDate() != null ? r.getFollowDate() : 0L))
                .orElse(null);
            status.put("smsPushed", latestSms != null);
            status.put("smsFrozen", latestSms != null && latestSms.getNextFollowDate() != null
                && latestSms.getNextFollowDate() > now);
            status.put("smsCooldownUntil", latestSms != null && latestSms.getNextFollowDate() != null
                ? latestSms.getNextFollowDate() : 0);

            HealthFollowRecord latestPhone = all.stream()
                .filter(r -> aid.equals(r.getCustomerId()) && "PHONE_FOLLOW".equals(r.getFollowType()))
                .max(Comparator.comparingLong(r -> r.getFollowDate() != null ? r.getFollowDate() : 0L))
                .orElse(null);
            status.put("phoneContacted", latestPhone != null);
            status.put("phoneFrozen", latestPhone != null && latestPhone.getNextFollowDate() != null
                && latestPhone.getNextFollowDate() > now);
            status.put("phoneCooldownUntil", latestPhone != null && latestPhone.getNextFollowDate() != null
                ? latestPhone.getNextFollowDate() : 0);

            result.put(aid, status);
        }
        return result;
    }
}
