import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
import { getReasonConfig } from '@/api/modules';
const dicApprovalKeyMap = {
    [FormDesignKeyEnum.CONTRACT]: ReasonTypeEnum.CONTRACT_APPROVAL,
    [FormDesignKeyEnum.INVOICE]: ReasonTypeEnum.INVOICE_APPROVAL,
    [FormDesignKeyEnum.CONTRACT_INVOICE]: ReasonTypeEnum.INVOICE_APPROVAL,
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: ReasonTypeEnum.QUOTATION_APPROVAL,
};
export default function useApprovalConfig(formKey) {
    const reasonOptions = ref([]);
    const dicApprovalEnable = ref(false);
    const dicKey = ref();
    dicKey.value = dicApprovalKeyMap[formKey];
    async function initApprovalConfig() {
        if (!dicKey.value)
            return;
        try {
            const { dictList, enable } = await getReasonConfig(dicKey.value);
            reasonOptions.value = dictList.map((e) => ({ label: e.name, value: e.id }));
            dicApprovalEnable.value = enable;
        }
        catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
        }
    }
    return {
        initApprovalConfig,
        dicApprovalEnable,
    };
}
//# sourceMappingURL=useApprovalConfig.js.map