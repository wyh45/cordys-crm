import { useI18n } from '@lib/shared/hooks/useI18n';
import { FormulaErrorCode } from '../../config';
import { isColumnField } from './rule-utils';
const { t } = useI18n();
export const DAYS_RULE = {
    name: 'DAYS',
    diagnose({ fnNode, args }) {
        const diagnostics = [];
        /**  参数类型校验（逐个） */
        args.forEach((arg, index) => {
            if (arg.type === 'field' && isColumnField(arg.fieldId)) {
                diagnostics.push({
                    type: 'error',
                    code: FormulaErrorCode.INVALID_FUNCTION_CALL,
                    functionName: fnNode.name,
                    message: t('formulaEditor.diagnostics.invalidArgOfDAYS', {
                        index: index + 1,
                    }),
                    highlight: {
                        tokenRange: [arg.startTokenIndex, arg.endTokenIndex],
                    },
                });
            }
        });
        /**  参数个数校验 */
        if (args?.length !== 2) {
            diagnostics.push({
                type: 'error',
                code: FormulaErrorCode.ARG_COUNT_ERROR,
                functionName: fnNode.name,
                message: t('formulaEditor.diagnostics.argCountErrorOfDAYS'),
                highlight: {
                    tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
                },
            });
            return diagnostics;
        }
        return diagnostics;
    },
};
export default DAYS_RULE;
//# sourceMappingURL=days.rule.js.map