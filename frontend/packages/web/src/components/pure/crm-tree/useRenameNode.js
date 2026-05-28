import { useI18n } from '@lib/shared/hooks/useI18n';
import NodeTitle from './nodeTitle.vue';
import useDiscreteApi from '@/hooks/useDiscreteApi';
/**
 *
 * @param renameApi 重命名API
 * @param fieldNames 字段配置
 * @returns
 */
export default function useRenameNode(getSiblingLabelsFun, renameApi, createApi, cancelNodeCreateFun, renameStatic, fieldNames = { keyField: 'key', labelField: 'label', childrenField: 'children' }) {
    const { t } = useI18n();
    const { message } = useDiscreteApi();
    // 切换编辑模式 管理节点的编辑状态
    const editingKey = ref('');
    const { keyField, labelField } = fieldNames;
    const getEditingMode = (key) => (editingKey.value === key ? 'rename' : 'view');
    const toggleEdit = (key) => {
        if (editingKey.value === key) {
            editingKey.value = '';
        }
        else {
            editingKey.value = key;
        }
    };
    /** 处理重命名逻辑 */
    const loading = ref(false);
    async function handleRenameMode(node) {
        if (!renameApi)
            return;
        loading.value = true;
        try {
            const res = await renameApi(node);
            if (res) {
                toggleEdit(node[keyField]);
                node.hideMoreAction = false;
                message.success(t('common.updateSuccess'));
            }
        }
        catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
        }
        finally {
            loading.value = false;
        }
    }
    /** 创建节点 */
    async function handleCreateMode(node) {
        if (!createApi)
            return;
        loading.value = true;
        try {
            const res = await createApi(node);
            if (res) {
                toggleEdit(node[keyField]);
                node.hideMoreAction = false;
                message.success(t('common.addSuccess'));
            }
        }
        catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
        }
        finally {
            loading.value = false;
        }
    }
    // 处理编辑
    function handleAction(node, newLabel, notChange) {
        const key = node[keyField];
        node[labelField] = newLabel;
        if (node.isNew && !renameStatic?.value) {
            handleCreateMode(node);
            // 有改变且为重命名
        }
        else if (!notChange && getEditingMode(key) === 'rename' && !renameStatic?.value) {
            handleRenameMode(node);
            // 否则切换预览模式
        }
        else {
            toggleEdit(key);
            node.hideMoreAction = false;
        }
    }
    const siblingLabels = computed(() => {
        return getSiblingLabelsFun(editingKey.value);
    });
    /** 生成 NodeTitle  */
    function createEditInput(infoProps, fieldConfig, renderLabel, titleProps) {
        const { option, selected, checked } = infoProps;
        const mode = getEditingMode(option[keyField]);
        return h(NodeTitle, {
            mode,
            fieldConfig,
            allNames: siblingLabels.value,
            loading: loading.value,
            class: selected ? 'crm-select-label' : '',
            ...titleProps,
            allowCancel: option.isNew && !renameStatic?.value,
            onSave: (newLabel, notChange) => handleAction(option, newLabel, notChange),
            onCancel: () => {
                // 取消创建该节点
                if (option.isNew && !renameStatic?.value && typeof cancelNodeCreateFun === 'function') {
                    cancelNodeCreateFun(option);
                }
                toggleEdit(option[keyField]);
                option.hideMoreAction = false;
            },
        }, {
            labelSlot: () => renderLabel?.({ option, editing: mode !== 'view', selected, checked }),
        });
    }
    return { toggleEdit, createEditInput, editingKey };
}
//# sourceMappingURL=useRenameNode.js.map