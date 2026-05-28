import { useI18n } from '@lib/shared/hooks/useI18n';
import { FormulaErrorCode } from '../../config';
const { t } = useI18n();
export const TODAY_RULE = {
    name: 'TODAY',
    diagnose({ fnNode, args }) {
        const diagnostics = [];
        if (args?.length !== 0) {
            diagnostics.push({
                type: 'error',
                code: FormulaErrorCode.ARG_COUNT_ERROR,
                functionName: fnNode.name,
                message: t('formulaEditor.diagnostics.nowNotAcceptParams', {
                    name: fnNode.name,
                }),
                highlight: {
                    tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
                },
            });
        }
        return diagnostics;
    },
};
export default TODAY_RULE;
//# sourceMappingURL=today.rule.js.map