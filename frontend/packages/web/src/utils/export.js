import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
export function getColumnType(item, customFieldsFilterConfig, supportShowFields) {
    if (customFieldsFilterConfig?.some((i) => i.dataIndex === item.key)) {
        return ColumnTypeEnum.CUSTOM;
    }
    if (supportShowFields && item.resourceFieldId) {
        return ColumnTypeEnum.SHOW_FIELD;
    }
    return ColumnTypeEnum.SYSTEM;
}
export function getExportColumns(allColumns, customFieldsFilterConfig, fieldList, supportShowFields) {
    const result = allColumns
        .filter((item) => item.key !== 'operation' &&
        item.type !== 'selection' &&
        item.key !== 'crmTableOrder' &&
        item.filedType !== FieldTypeEnum.PICTURE &&
        (supportShowFields || !item.resourceFieldId) // 部分表单支持导出显示字段
    )
        .map((e) => {
        return {
            key: e.key?.toString() || '',
            title: e.title || '',
            columnType: getColumnType(e, customFieldsFilterConfig, supportShowFields),
        };
    });
    const subCol = fieldList?.filter((i) => [FieldTypeEnum.SUB_PRODUCT, FieldTypeEnum.SUB_PRICE].includes(i.type));
    subCol?.forEach((j) => {
        result.push({
            key: j.businessKey ?? j.id,
            title: j.name,
            columnType: ColumnTypeEnum.CUSTOM,
        });
    });
    return result;
}
//# sourceMappingURL=export.js.map