package cn.cordys.crm.system.notice.sender.sms;

import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.MessageDetailDTO;
import cn.cordys.crm.system.notice.common.NoticeModel;
import cn.cordys.crm.system.notice.common.Receiver;
import cn.cordys.crm.system.notice.sender.AbstractNoticeSender;
import cn.cordys.crm.system.utils.SmsSender;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SmsNoticeSender extends AbstractNoticeSender {

    @Resource
    private SmsSender smsSender;

    @Override
    public void send(MessageDetailDTO messageDetailDTO, NoticeModel noticeModel) {
        List<Receiver> receivers = super.getReceivers(
                noticeModel.getReceivers(),
                noticeModel.isExcludeSelf(),
                noticeModel.getOperator()
        );
        if (CollectionUtils.isEmpty(receivers)) {
            log.debug("短信没有接收人，跳过发送");
            return;
        }

        String context = super.getContext(messageDetailDTO, noticeModel);
        String[] templateParams = buildTemplateParams(context, noticeModel);

        String[] phoneNumbers = buildPhoneNumbers(receivers, messageDetailDTO.getOrganizationId());
        if (phoneNumbers == null || phoneNumbers.length == 0) {
            log.debug("短信没有有效手机号，跳过发送");
            return;
        }

        try {
            smsSender.sendBatch(phoneNumbers, templateParams);
            log.debug("短信发送完成");
        } catch (Exception e) {
            log.error("短信发送失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 Receiver 列表中提取用户手机号
     */
    private String[] buildPhoneNumbers(List<Receiver> receivers, String organizationId) {
        List<String> userIds = receivers.stream()
                .map(Receiver::getUserId)
                .distinct()
                .toList();

        List<User> users = super.getUsers(userIds, organizationId);

        return users.stream()
                .map(User::getPhone)
                .filter(StringUtils::isNotBlank)
                .map(this::normalizePhoneNumber)
                .toArray(String[]::new);
    }

    /**
     * 将手机号转换为 E.164 格式（如果还不是）
     * 例如：13812345678 -> +8613812345678
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }
        phone = phone.trim();
        // 已经是 E.164 格式
        if (phone.startsWith("+")) {
            return phone;
        }
        // 移除前导 0
        if (phone.startsWith("0")) {
            phone = phone.substring(1);
        }
        // 添加中国区号
        if (!phone.startsWith("86")) {
            phone = "86" + phone;
        }
        return "+" + phone;
    }

    /**
     * 将 context 内容拆分为模板参数
     * 目前短信内容直接作为模板第一个参数发送
     */
    private String[] buildTemplateParams(String context, NoticeModel noticeModel) {
        if (StringUtils.isBlank(context)) {
            return null;
        }
        // 腾讯云短信模板参数是 String[] 格式
        // 目前 context 可能包含多个 ${变量}，这里做简化处理
        // 实际使用时建议在 NoticeModel.paramMap 中传入独立的模板参数
        return new String[]{context};
    }
}
