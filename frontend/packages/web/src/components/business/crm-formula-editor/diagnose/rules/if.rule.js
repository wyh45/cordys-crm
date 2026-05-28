import { useI18n } from '@lib/shared/hooks/useI18n';
import { FormulaErrorCode } from '../../config';
const { t } = useI18n();
export const IF_RULE = {
    name: 'IF',
    diagnose({ fnNode, args }) {
        const diagnostics = [];
        if (args.length < 2 || args.length > 3) {
            diagnostics.push({
                type: 'error',
                code: FormulaErrorCode.ARG_COUNT_ERROR,
                functionName: fnNode.name,
                message: t('formulaEditor.diagnostics.argCountErrorOfIF'),
                highlight: {
                    tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
                },
            });
        }
        return diagnostics;
    },
};
export default IF_RULE;
//# sourceMappingURL=if.rule.js.map