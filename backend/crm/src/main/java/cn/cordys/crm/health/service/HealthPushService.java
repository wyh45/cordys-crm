package cn.cordys.crm.health.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.health.domain.HealthArchive;
import cn.cordys.crm.health.domain.HealthKnowledge;
import cn.cordys.crm.health.domain.HealthPushRecord;
import cn.cordys.crm.health.dto.HealthPushRequest;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.crm.system.notice.NoticeSendService;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.utils.SmsSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 健康推送服务
 * 负责将健康知识推送给指定客户，并记录推送历史
 * 
 * 推送渠道说明：
 * - SMS: 直接调 SmsSender（HealthArchive 有手机号，不依赖CRM User表）
 * - 其他渠道(INSITE/EMAIL/DINGTALK/LARK/WECOM): 走 NoticeSendService（需CRM UserId）
 */
@Service
@Slf4j
public class HealthPushService {

    private static final String CHANNEL_SMS = "SMS";
    private static final String CHANNEL_INSITE = "INSITE";
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_DINGTALK = "DINGTALK";
    private static final String CHANNEL_LARK = "LARK";
    private static final String CHANNEL_WECOM = "WECOM";

    @Resource
    private BaseMapper<HealthArchive> archiveMapper;

    @Resource
    private BaseMapper<HealthKnowledge> knowledgeMapper;

    @Resource
    private BaseMapper<HealthPushRecord> pushRecordMapper;

    @Resource
    private NoticeSendService noticeSendService;

    @Resource
    private SmsSender smsSender;

    @Value("${health.push.module:health}")
    private String module;

    /**
     * 推送健康知识给客户
     */
    @Async("threadPoolTaskExecutor")
    public void pushKnowledge(HealthPushRequest request, String operatorId, String organizationId) {
        String knowledgeId = request.getKnowledgeId();
        if (StringUtils.isBlank(knowledgeId)) {
            log.warn("知识ID为空，跳过推送");
            return;
        }

        HealthKnowledge knowledge = knowledgeMapper.selectByPrimaryKey(knowledgeId);
        if (knowledge == null) {
            log.warn("知识库记录不存在，knowledgeId={}", knowledgeId);
            return;
        }

        String title = StringUtils.isNotBlank(request.getTitle()) ? request.getTitle() : knowledge.getTitle();
        String content = StringUtils.isNotBlank(request.getContent()) ? request.getContent() : knowledge.getContent();
        String channel = StringUtils.upperCase(request.getChannel());

        List<HealthArchive> customers = resolveCustomers(request.getCustomerIds());
        if (customers.isEmpty()) {
            log.info("没有需要推送的客户，knowledgeId={}", knowledgeId);
            return;
        }

        log.info("开始推送健康知识，knowledgeId={}, channel={}, 客户数={}", knowledgeId, channel, customers.size());

        if (CHANNEL_SMS.equals(channel)) {
            pushBySms(title, content, customers, knowledgeId, operatorId);
        } else {
            pushByNoticeSystem(title, content, channel, customers, knowledgeId, operatorId, organizationId);
        }

        for (HealthArchive customer : customers) {
            savePushRecord(customer.getId(), knowledgeId, channel, operatorId);
        }

        log.info("推送完成，knowledgeId={}, channel={}", knowledgeId, channel);
    }

    /**
     * 短信推送：直接调 SmsSender（不依赖CRM User表）
     */
    private void pushBySms(String title, String content, List<HealthArchive> customers,
                            String knowledgeId, String operatorId) {
        List<String> phones = customers.stream()
                .map(HealthArchive::getPhone)
                .filter(StringUtils::isNotBlank)
                .map(this::normalizePhoneNumber)
                .distinct()
                .toList();

        if (phones.isEmpty()) {
            log.warn("没有有效手机号，knowledgeId={}", knowledgeId);
            return;
        }

        String fullContent = title + "：" + content;
        smsSender.sendBatch(phones.toArray(new String[0]), new String[]{fullContent});
        log.info("短信推送完成，knowledgeId={}, 发送数={}", knowledgeId, phones.size());
    }

    /**
     * 其他渠道：走 NoticeSendService（依赖 HealthArchive.customerId = CRM UserId）
     */
    private void pushByNoticeSystem(String title, String content, String channel,
                                     List<HealthArchive> customers, String knowledgeId,
                                     String operatorId, String organizationId) {
        List<Receiver> receivers = new ArrayList<>();
        for (HealthArchive customer : customers) {
            if (StringUtils.isNotBlank(customer.getCustomerId())) {
                receivers.add(new Receiver(customer.getCustomerId(), null));
            }
        }

        if (receivers.isEmpty()) {
            log.warn("没有有效接收人，channel={}, knowledgeId={}", channel, knowledgeId);
            return;
        }

        NoticeModel noticeModel = NoticeModel.builder()
                .subject(title)
                .context(content)
                .event(channel)
                .receivers(receivers)
                .paramMap(Map.of(
                        "Language", "SIMPLIFIED_CHINESE",
                        "organizationId", organizationId != null ? organizationId : ""
                ))
                .build();

        noticeSendService.send(module, noticeModel);
        log.info("{}推送完成，knowledgeId={}, 接收人数={}", channel, knowledgeId, receivers.size());
    }

    /**
     * 获取推送记录（分页）
     */
    public List<HealthPushRecord> listPushRecords(int pageNum, int pageSize) {
        List<HealthPushRecord> all = pushRecordMapper.select(new HealthPushRecord());
        all.sort((a, b) -> Long.compare(
                b.getPushTime() != null ? b.getPushTime() : 0L,
                a.getPushTime() != null ? a.getPushTime() : 0L
        ));
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        if (start >= all.size()) return List.of();
        return all.subList(start, end);
    }

    public List<HealthPushRecord> getByCustomerId(String customerId) {
        HealthPushRecord criteria = new HealthPushRecord();
        criteria.setCustomerId(customerId);
        return pushRecordMapper.select(criteria);
    }

    public List<HealthPushRecord> getByKnowledgeId(String knowledgeId) {
        HealthPushRecord criteria = new HealthPushRecord();
        criteria.setKnowledgeId(knowledgeId);
        return pushRecordMapper.select(criteria);
    }

    private List<HealthArchive> resolveCustomers(List<String> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return archiveMapper.select(new HealthArchive());
        }
        List<HealthArchive> result = new ArrayList<>();
        for (String id : customerIds) {
            HealthArchive archive = archiveMapper.selectByPrimaryKey(id);
            if (archive != null) {
                result.add(archive);
            }
        }
        return result;
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        phone = phone.trim();
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) phone = phone.substring(1);
        if (!phone.startsWith("86")) phone = "86" + phone;
        return "+" + phone;
    }

    @Transactional
    private void savePushRecord(String customerId, String knowledgeId, String channel, String operatorId) {
        try {
            HealthPushRecord record = new HealthPushRecord();
            record.setId(IDGenerator.nextStr());
            record.setCustomerId(customerId);
            record.setKnowledgeId(knowledgeId);
            record.setPushChannel(channel);
            record.setPushStatus("SENT");
            record.setPushTime(System.currentTimeMillis());
            record.setCreateUser(operatorId);
            record.setCreateTime(System.currentTimeMillis());
            record.setUpdateTime(System.currentTimeMillis());
            pushRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存推送记录失败 customerId={}", customerId, e);
        }
    }
}
