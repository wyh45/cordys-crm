import { safeParseFormula } from '@/components/business/crm-formula-editor/utils';
import { flatAllFields } from '../../utils';
/**
 * 一次性获取公式展示所需信息
 */
export function getFormulaDisplayInfo(options) {
    const { formula, fields, invalidText, emptyText, isSubTableRender } = options;
    const allFieldIds = flatAllFields(fields, {
        isSubTableRender,
    }).map((e) => e.id);
    if (!formula) {
        return {
            allFieldIds,
            display: '',
            isInvalid: false,
            tooltip: emptyText,
        };
    }
    const parsed = safeParseFormula(formula);
    const display = parsed.display ?? '';
    const savedFields = parsed.fields?.map((e) => e.fieldId) ?? [];
    const isInvalid = savedFields.some((fieldId) => !allFieldIds.includes(fieldId));
    let tooltip;
    if (!display) {
        tooltip = emptyText;
    }
    else if (isInvalid) {
        tooltip = invalidText;
    }
    else {
        tooltip = display;
    }
    return {
        allFieldIds,
        display,
        isInvalid,
        tooltip,
    };
}
//# sourceMappingURL=formula-display.js.map