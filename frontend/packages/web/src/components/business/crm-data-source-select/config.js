import { FieldDataSourceTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { getCustomerOptions, getFieldBusinessTitleList, getFieldClueList, getFieldContactList, getFieldContractList, getFieldContractPaymentPlanList, getFieldContractPaymentRecordList, getFieldCustomerList, getFieldOpportunityList, getFieldOrderList, getFieldPriceList, getFieldProductList, getFieldQuotationList, getUserOptions, } from '@/api/modules';
export const sourceApi = {
    [FieldDataSourceTypeEnum.BUSINESS]: getFieldOpportunityList,
    [FieldDataSourceTypeEnum.CLUE]: getFieldClueList,
    [FieldDataSourceTypeEnum.CONTACT]: getFieldContactList,
    [FieldDataSourceTypeEnum.CUSTOMER]: getFieldCustomerList,
    [FieldDataSourceTypeEnum.PRODUCT]: getFieldProductList,
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: getCustomerOptions,
    [FieldDataSourceTypeEnum.USER_OPTIONS]: getUserOptions,
    [FieldDataSourceTypeEnum.CONTRACT]: getFieldContractList,
    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT]: getFieldContractPaymentPlanList,
    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT_RECORD]: getFieldContractPaymentRecordList,
    [FieldDataSourceTypeEnum.PRICE]: getFieldPriceList,
    [FieldDataSourceTypeEnum.QUOTATION]: getFieldQuotationList,
    [FieldDataSourceTypeEnum.BUSINESS_TITLE]: getFieldBusinessTitleList,
    [FieldDataSourceTypeEnum.ORDER]: getFieldOrderList,
};
export const formKeyMap = {
    [FieldDataSourceTypeEnum.BUSINESS]: FormDesignKeyEnum.BUSINESS,
    [FieldDataSourceTypeEnum.CLUE]: FormDesignKeyEnum.CLUE,
    [FieldDataSourceTypeEnum.CONTACT]: FormDesignKeyEnum.CONTACT,
    [FieldDataSourceTypeEnum.CUSTOMER]: FormDesignKeyEnum.CUSTOMER,
    [FieldDataSourceTypeEnum.PRODUCT]: FormDesignKeyEnum.PRODUCT,
    [FieldDataSourceTypeEnum.CONTRACT]: FormDesignKeyEnum.CONTRACT,
    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT]: FormDesignKeyEnum.CONTRACT_PAYMENT,
    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT_RECORD]: FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD,
    [FieldDataSourceTypeEnum.PRICE]: FormDesignKeyEnum.PRICE,
    [FieldDataSourceTypeEnum.QUOTATION]: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    [FieldDataSourceTypeEnum.BUSINESS_TITLE]: FormDesignKeyEnum.BUSINESS_TITLE,
    [FieldDataSourceTypeEnum.ORDER]: FormDesignKeyEnum.ORDER,
};
//# sourceMappingURL=config.js.map