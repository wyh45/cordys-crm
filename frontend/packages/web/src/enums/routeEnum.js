export var SystemRouteEnum;
(function (SystemRouteEnum) {
    SystemRouteEnum["SYSTEM"] = "system";
    SystemRouteEnum["SYSTEM_ORG"] = "systemOrg";
    SystemRouteEnum["SYSTEM_ROLE"] = "systemRole";
    SystemRouteEnum["SYSTEM_MODULE"] = "systemModule";
    SystemRouteEnum["SYSTEM_BUSINESS"] = "systemBusiness";
    SystemRouteEnum["SYSTEM_LICENSE"] = "systemLicense";
    SystemRouteEnum["SYSTEM_LOG"] = "systemLog";
    SystemRouteEnum["SYSTEM_MESSAGE"] = "systemMessage";
})(SystemRouteEnum || (SystemRouteEnum = {}));
export var OpportunityRouteEnum;
(function (OpportunityRouteEnum) {
    OpportunityRouteEnum["OPPORTUNITY"] = "opportunity";
    OpportunityRouteEnum["OPPORTUNITY_OPT"] = "opportunityOpt";
    OpportunityRouteEnum["OPPORTUNITY_QUOTATION"] = "opportunityQuotation";
})(OpportunityRouteEnum || (OpportunityRouteEnum = {}));
export var ClueRouteEnum;
(function (ClueRouteEnum) {
    ClueRouteEnum["CLUE_MANAGEMENT"] = "leadManagement";
    ClueRouteEnum["CLUE_MANAGEMENT_CLUE"] = "leadManagementLead";
    ClueRouteEnum["CLUE_MANAGEMENT_POOL"] = "leadManagementPool";
})(ClueRouteEnum || (ClueRouteEnum = {}));
export var CustomerRouteEnum;
(function (CustomerRouteEnum) {
    CustomerRouteEnum["CUSTOMER"] = "account";
    CustomerRouteEnum["CUSTOMER_INDEX"] = "accountIndex";
    CustomerRouteEnum["CUSTOMER_CONTACT"] = "accountContact";
    CustomerRouteEnum["CUSTOMER_OPEN_SEA"] = "accountOpenSea";
})(CustomerRouteEnum || (CustomerRouteEnum = {}));
export var ContractRouteEnum;
(function (ContractRouteEnum) {
    ContractRouteEnum["CONTRACT"] = "contract";
    ContractRouteEnum["CONTRACT_INDEX"] = "contractIndex";
    ContractRouteEnum["CONTRACT_PAYMENT"] = "contractPaymentPlan";
    ContractRouteEnum["CONTRACT_PAYMENT_RECORD"] = "contractPaymentRecord";
    ContractRouteEnum["CONTRACT_BUSINESS_NAME"] = "contractBusinessName";
    ContractRouteEnum["CONTRACT_INVOICE"] = "contractInvoice";
})(ContractRouteEnum || (ContractRouteEnum = {}));
export var OrderRouteEnum;
(function (OrderRouteEnum) {
    OrderRouteEnum["ORDER"] = "order";
    OrderRouteEnum["ORDER_INDEX"] = "orderIndex";
})(OrderRouteEnum || (OrderRouteEnum = {}));
export var ProductRouteEnum;
(function (ProductRouteEnum) {
    ProductRouteEnum["PRODUCT"] = "product";
    ProductRouteEnum["PRODUCT_PRO"] = "productPro";
    ProductRouteEnum["PRODUCT_PRICE"] = "productPrice";
})(ProductRouteEnum || (ProductRouteEnum = {}));
export var PersonalRouteEnum;
(function (PersonalRouteEnum) {
    PersonalRouteEnum["PERSONAL_INFO"] = "personalInfo";
    PersonalRouteEnum["PERSONAL_PLAN"] = "personalPlan";
    PersonalRouteEnum["PERSONAL_EXPORT"] = "personalExport";
    PersonalRouteEnum["LOGOUT"] = "logout";
})(PersonalRouteEnum || (PersonalRouteEnum = {}));
export var WorkbenchRouteEnum;
(function (WorkbenchRouteEnum) {
    WorkbenchRouteEnum["WORKBENCH"] = "workbench";
    WorkbenchRouteEnum["WORKBENCH_INDEX"] = "workbenchIndex";
})(WorkbenchRouteEnum || (WorkbenchRouteEnum = {}));
export var AgentRouteEnum;
(function (AgentRouteEnum) {
    AgentRouteEnum["AGENT"] = "agent";
    AgentRouteEnum["AGENT_INDEX"] = "agentIndex";
})(AgentRouteEnum || (AgentRouteEnum = {}));
export var DashboardRouteEnum;
(function (DashboardRouteEnum) {
    DashboardRouteEnum["DASHBOARD"] = "dashboard";
    DashboardRouteEnum["DASHBOARD_INDEX"] = "dashboardIndex";
    DashboardRouteEnum["DASHBOARD_LINK"] = "dashboardLink";
    DashboardRouteEnum["DASHBOARD_MODULE"] = "dashboardModule";
})(DashboardRouteEnum || (DashboardRouteEnum = {}));
export var TenderRouteEnum;
(function (TenderRouteEnum) {
    TenderRouteEnum["TENDER"] = "tender";
    TenderRouteEnum["TENDER_INDEX"] = "tenderIndex";
})(TenderRouteEnum || (TenderRouteEnum = {}));
export var HealthRouteEnum;
(function (HealthRouteEnum) {
    HealthRouteEnum["HEALTH"] = "health";
    HealthRouteEnum["HEALTH_ARCHIVE"] = "healthArchive";
    HealthRouteEnum["HEALTH_EXAMINATION"] = "healthExamination";
    HealthRouteEnum["HEALTH_FOLLOW"] = "healthFollow";
    HealthRouteEnum["HEALTH_KNOWLEDGE"] = "healthKnowledge";
    HealthRouteEnum["HEALTH_AI"] = "healthAi";
})(HealthRouteEnum || (HealthRouteEnum = {}));
export var FullPageEnum;
(function (FullPageEnum) {
    FullPageEnum["FULL_PAGE"] = "fullPage";
    FullPageEnum["FULL_PAGE_DASHBOARD"] = "fullPageDashboard";
    FullPageEnum["FULL_PAGE_EXPORT_QUOTATION"] = "fullPageExportQuotation";
    FullPageEnum["FULL_PAGE_EXPORT_ORDER"] = "fullPageExportOrder";
})(FullPageEnum || (FullPageEnum = {}));
export const AppRouteEnum = {
    ...SystemRouteEnum,
    ...OpportunityRouteEnum,
    ...ClueRouteEnum,
    ...CustomerRouteEnum,
    ...ProductRouteEnum,
    ...PersonalRouteEnum,
    ...WorkbenchRouteEnum,
    ...DashboardRouteEnum,
    ...AgentRouteEnum,
    ...ContractRouteEnum,
    ...OrderRouteEnum,
    ...TenderRouteEnum,
    ...HealthRouteEnum,
};
//# sourceMappingURL=routeEnum.js.map