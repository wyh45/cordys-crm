import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
export default function useFormCreateFilter() {
    const customFieldsFilterConfig = ref([]);
    // 获取配置属性
    function getFilterListConfig(res, addDefaultKeyAsId = false) {
        const getConfigProps = (field) => {
            if ([FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX].includes(field.type)) {
                return {
                    selectProps: {
                        options: field.options,
                        multiple: true,
                    },
                };
            }
            if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)) {
                return {
                    dataSourceProps: {
                        dataSourceType: field.dataSourceType,
                        maxTagCount: 'responsive',
                    },
                };
            }
            // TODO: 其他类型
            return {};
        };
        return (res.fields || [])
            .filter((e) => !e.resourceFieldId)
            .reduce((acc, field) => {
            if (![
                FieldTypeEnum.TEXTAREA,
                FieldTypeEnum.PICTURE,
                FieldTypeEnum.DIVIDER,
                FieldTypeEnum.SUB_PRICE,
                FieldTypeEnum.SUB_PRODUCT,
            ].includes(field.type)) {
                let key = field.businessKey || field.id;
                if (field.resourceFieldId) {
                    // 数据源引用字段用 id作为 key
                    key = field.id;
                }
                acc.push({
                    title: field.name,
                    dataIndex: key,
                    type: field.type,
                    ...(addDefaultKeyAsId ? { id: field.id } : {}),
                    ...getConfigProps(field),
                });
            }
            return acc;
        }, []);
    }
    return {
        getFilterListConfig,
        customFieldsFilterConfig,
    };
}
//# sourceMappingURL=useFormCreateAdvanceFilter.js.map