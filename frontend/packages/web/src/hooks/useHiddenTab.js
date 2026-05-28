import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getClueTab, getContractTab, getCustomerContactTab, getCustomerTab, getFollowPlanTab, getFollowRecordTab, getInvoicedTab, getOptTab, getOrderTab, getPaymentPlanTab, getPaymentRecordTab, getQuotationTab, } from '@/api/modules';
export default function useHiddenTab(type) {
    const { t } = useI18n();
    const tabApiMap = {
        [FormDesignKeyEnum.CUSTOMER]: getCustomerTab,
        [FormDesignKeyEnum.CONTACT]: getCustomerContactTab,
        [FormDesignKeyEnum.BUSINESS]: getOptTab,
        [FormDesignKeyEnum.CLUE]: getClueTab,
        [FormDesignKeyEnum.FOLLOW_PLAN]: getFollowPlanTab,
        [FormDesignKeyEnum.FOLLOW_RECORD]: getFollowRecordTab,
        [FormDesignKeyEnum.CONTRACT]: getContractTab,
        [FormDesignKeyEnum.CONTRACT_PAYMENT]: getPaymentPlanTab,
        [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: getPaymentRecordTab,
        [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: getQuotationTab,
        [FormDesignKeyEnum.INVOICE]: getInvoicedTab,
        [FormDesignKeyEnum.ORDER]: getOrderTab,
    };
    const allClueTabList = [
        {
            name: CustomerSearchTypeEnum.ALL,
            tab: t('clue.allClues'),
        },
        {
            name: CustomerSearchTypeEnum.SELF,
            tab: t('clue.myClues'),
        },
        {
            name: CustomerSearchTypeEnum.DEPARTMENT,
            tab: t('clue.departmentClues'),
        },
    ];
    const allContactTabList = [
        {
            name: CustomerSearchTypeEnum.ALL,
            tab: t('customer.contacts.all'),
        },
        {
            name: CustomerSearchTypeEnum.SELF,
            tab: t('customer.contacts.mine'),
        },
        {
            name: CustomerSearchTypeEnum.DEPARTMENT,
            tab: t('customer.contacts.department'),
        },
    ];
    const allCustomerTabList = [
        {
            name: CustomerSearchTypeEnum.ALL,
            tab: t('customer.all'),
        },
        {
            name: CustomerSearchTypeEnum.SELF,
            tab: t('customer.mine'),
        },
        {
            name: CustomerSearchTypeEnum.DEPARTMENT,
            tab: t('customer.deptCustomer'),
        },
        {
            name: CustomerSearchTypeEnum.CUSTOMER_COLLABORATION,
            tab: t('customer.cooperationCustomer'),
        },
    ];
    const allOpportunityTabList = [
        {
            name: OpportunitySearchTypeEnum.ALL,
            tab: t('opportunity.allOpportunities'),
        },
        {
            name: OpportunitySearchTypeEnum.SELF,
            tab: t('opportunity.myOpportunities'),
        },
        {
            name: OpportunitySearchTypeEnum.DEPARTMENT,
            tab: t('opportunity.departmentOpportunities'),
        },
        {
            name: OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS,
            tab: t('opportunity.convertedOpportunities'),
        },
    ];
    const allLeadPoolTabList = [
        {
            name: CustomerSearchTypeEnum.ALL,
            tab: t('clue.allClues'),
        },
    ];
    const allAccountPoolTabList = [
        {
            name: CustomerSearchTypeEnum.ALL,
            tab: t('customer.all'),
        },
    ];
    const allPlanTabList = [
        {
            name: OpportunitySearchTypeEnum.ALL,
            tab: t('eventDrawer.plan.all'),
        },
        {
            name: OpportunitySearchTypeEnum.SELF,
            tab: t('system.personal.plan'),
        },
        {
            name: OpportunitySearchTypeEnum.DEPARTMENT,
            tab: t('eventDrawer.plan.depart'),
        },
    ];
    const allRecordTabList = [
        {
            name: OpportunitySearchTypeEnum.ALL,
            tab: t('eventDrawer.record.all'),
        },
        {
            name: OpportunitySearchTypeEnum.SELF,
            tab: t('eventDrawer.record.my'),
        },
        {
            name: OpportunitySearchTypeEnum.DEPARTMENT,
            tab: t('eventDrawer.record.depart'),
        },
    ];
    const allContractTabList = [
        {
            name: OpportunitySearchTypeEnum.ALL,
            tab: t('contract.all'),
        },
        {
            name: OpportunitySearchTypeEnum.SELF,
            tab: t('contract.my'),
        },
        {
            name: OpportunitySearchTypeEnum.DEPARTMENT,
            tab: t('contract.depart'),
        },
    ];
    const allQuotationTabList = [
        {
            name: OpportunitySearchTypeEnum.ALL,
            tab: t('opportunity.quotation.all'),
        },
        {
            name: CustomerSearchTypeEnum.DEPARTMENT,
            tab: t('opportunity.quotation.department'),
        },
        {
            name: CustomerSearchTypeEnum.SELF,
            tab: t('opportunity.quotation.my'),
        },
    ];
    const allInvoiceTabList = [
        {
            name: OpportunitySearchTypeEnum.ALL,
            tab: t('invoice.all'),
        },
        {
            name: OpportunitySearchTypeEnum.SELF,
            tab: t('invoice.my'),
        },
        {
            name: OpportunitySearchTypeEnum.DEPARTMENT,
            tab: t('invoice.depart'),
        },
    ];
    const allOrderTabList = [
        {
            name: OpportunitySearchTypeEnum.ALL,
            tab: t('order.all'),
        },
        {
            name: OpportunitySearchTypeEnum.SELF,
            tab: t('order.my'),
        },
        {
            name: OpportunitySearchTypeEnum.DEPARTMENT,
            tab: t('order.depart'),
        },
    ];
    const tabListMap = {
        [FormDesignKeyEnum.CUSTOMER]: allCustomerTabList,
        [FormDesignKeyEnum.CONTACT]: allContactTabList,
        [FormDesignKeyEnum.BUSINESS]: allOpportunityTabList,
        [FormDesignKeyEnum.CLUE]: allClueTabList,
        [FormDesignKeyEnum.CLUE_POOL]: allLeadPoolTabList,
        [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: allAccountPoolTabList,
        [FormDesignKeyEnum.FOLLOW_PLAN]: allPlanTabList,
        [FormDesignKeyEnum.FOLLOW_RECORD]: allRecordTabList,
        [FormDesignKeyEnum.CONTRACT]: allContractTabList,
        [FormDesignKeyEnum.CONTRACT_PAYMENT]: allPlanTabList,
        [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: allQuotationTabList,
        [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: allRecordTabList,
        [FormDesignKeyEnum.INVOICE]: allInvoiceTabList,
        [FormDesignKeyEnum.ORDER]: allOrderTabList,
    };
    const tabList = ref([]);
    async function initTab() {
        try {
            if (!type)
                return;
            const typeValue = type;
            const tabApi = tabApiMap[typeValue];
            if (tabApi) {
                const result = await tabApi();
                const { all, dept } = result;
                tabList.value = tabListMap[typeValue].filter((e) => {
                    if (e.name === 'ALL')
                        return !!all;
                    if (e.name === 'DEPARTMENT')
                        return !!dept;
                    return true;
                });
            }
            else {
                tabList.value = tabListMap[type];
            }
        }
        catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
        }
    }
    return {
        tabList,
        initTab,
    };
}
//# sourceMappingURL=useHiddenTab.js.map