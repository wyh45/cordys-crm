import { computed, ref } from 'vue';
import { useMessage } from 'naive-ui';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getGenerateId } from '@lib/shared/method';
import { useStatusApiConfig, useStatusStrategyConfig, useStatusTextConfig } from './config';
function buildRollbackParams(form) {
    return {
        afootRollBack: form.runningStageRollback,
        endRollBack: form.completedStageRollback,
    };
}
export default function useStageConfig(type) {
    const { t } = useI18n();
    const message = useMessage();
    const textConfigMap = useStatusTextConfig();
    const apiConfigMap = useStatusApiConfig();
    const strategyConfigMap = useStatusStrategyConfig();
    const textConfig = computed(() => textConfigMap[type]);
    const apiConfig = computed(() => apiConfigMap[type]);
    const strategyConfig = computed(() => strategyConfigMap[type]);
    const formItemModel = computed(() => strategyConfig.value.formItemModel);
    const form = ref({
        runningStageRollback: true,
        completedStageRollback: false,
        list: [],
    });
    function createEmptyRow() {
        return {
            _key: getGenerateId(),
            name: '',
            rate: null,
            type: 'AFOOT',
            editing: true,
        };
    }
    function getDropdownOptions(element) {
        if (element.type === 'END')
            return [];
        const minAfootCount = form.value.list.filter((item) => item.type === 'AFOOT').length === 1;
        if (minAfootCount) {
            return [
                { label: t('module.businessManage.insetBefore'), key: 'before' },
                { label: t('module.businessManage.insetAfter'), key: 'after' },
            ];
        }
        if (form.value.list.length === 10) {
            return [
                {
                    label: t('common.delete'),
                    key: 'delete',
                    danger: true,
                    disabled: element.stageHasData,
                    tooltipContent: element.stageHasData ? textConfig.value.stageHasDataTip : '',
                },
            ];
        }
        return [
            { label: t('module.businessManage.insetBefore'), key: 'before' },
            { label: t('module.businessManage.insetAfter'), key: 'after' },
            { type: 'divider' },
            {
                label: t('common.delete'),
                key: 'delete',
                danger: true,
                disabled: element.stageHasData,
                tooltipContent: element.stageHasData ? textConfig.value.stageHasDataTip : '',
            },
        ];
    }
    async function init() {
        const res = await apiConfig.value.load();
        form.value = {
            runningStageRollback: res.afootRollBack,
            completedStageRollback: res.endRollBack,
            list: (res.stageConfigList || []).map((item) => ({
                ...item,
                _key: item.id,
                editing: false,
                draggable: item.type !== 'END',
                ...(strategyConfig.value.normalizeItem?.(item) || {}),
            })),
        };
    }
    async function handleSwitchChange() {
        await apiConfig.value.rollback(buildRollbackParams(form.value));
        message.success(t('common.operationSuccess'));
    }
    async function handleSave(element, done, index) {
        if (element.id) {
            await apiConfig.value.update(strategyConfig.value.buildUpdateParams(element));
        }
        else {
            await apiConfig.value.create(strategyConfig.value.buildCreateParams(element, {
                list: form.value.list,
                index,
            }));
        }
        done();
        await init();
        message.success(t('common.operationSuccess'));
    }
    function handleCancelRow(index) {
        form.value.list.splice(index, 1);
    }
    async function handleMoreSelect(action, element, batchFormRef) {
        const index = form.value.list.findIndex((item) => item.id === element.id);
        if (action.key === 'before') {
            batchFormRef?.formValidate?.(() => {
                form.value.list.splice(index, 0, createEmptyRow());
            });
            return;
        }
        if (action.key === 'after') {
            batchFormRef?.formValidate?.(() => {
                form.value.list.splice(index + 1, 0, createEmptyRow());
            });
            return;
        }
        if (action.key === 'delete') {
            await apiConfig.value.remove(element.id);
            form.value.list.splice(index, 1);
            message.success(t('common.operationSuccess'));
        }
    }
    async function dragEnd(event) {
        if (form.value.list.length === 1)
            return;
        const { newIndex, oldIndex } = event;
        if (newIndex === oldIndex)
            return;
        const newList = [...form.value.list];
        const [movedItem] = newList.splice(oldIndex, 1);
        newList.splice(newIndex, 0, movedItem);
        await apiConfig.value.sort(newList.map((e) => e.id));
        await init();
        message.success(t('common.operationSuccess'));
    }
    function handleMove(evt) {
        const children = Array.from(evt.from.children);
        const draggedIndex = children.indexOf(evt.dragged);
        const targetIndex = children.indexOf(evt.related);
        if (draggedIndex >= form.value.list.length - 2)
            return false;
        if (targetIndex >= form.value.list.length - 2)
            return false;
        return true;
    }
    return {
        textConfig,
        formItemModel,
        form,
        init,
        handleSave,
        handleCancelRow,
        handleSwitchChange,
        handleMoreSelect,
        dragEnd,
        handleMove,
        getDropdownOptions,
    };
}
//# sourceMappingURL=useStageConfig.js.map