import { useDialog } from 'naive-ui';
export default function useModal() {
    const dialog = useDialog();
    return {
        openModal: (options) => {
            const defaultButtonProps = {
                positiveButtonProps: {
                    size: 'medium',
                    quaternary: options.mode === 'weak',
                    class: options.mode === 'weak' ? `text-btn-${options.type}` : '',
                },
                negativeButtonProps: {
                    size: 'medium',
                    secondary: options.mode !== 'weak',
                    ghost: false,
                    disabled: false,
                    quaternary: options.mode === 'weak',
                },
            };
            const dialogReactive = dialog.create({
                maskClosable: false,
                ...defaultButtonProps,
                class: `${options.class} crm-modal-${options.size || 'small'}`,
                ...options,
                onPositiveClick: async () => {
                    try {
                        dialogReactive.loading = true;
                        if (dialogReactive.negativeButtonProps) {
                            dialogReactive.negativeButtonProps.disabled = true;
                        }
                        if (options.onPositiveClick) {
                            await options.onPositiveClick();
                        }
                    }
                    catch (error) {
                        // eslint-disable-next-line no-console
                        console.error(error);
                    }
                    finally {
                        dialogReactive.loading = false;
                        if (dialogReactive.negativeButtonProps) {
                            dialogReactive.negativeButtonProps.disabled = false;
                        }
                    }
                },
                onClose: () => {
                    return !dialogReactive.loading;
                },
            });
        },
    };
}
//# sourceMappingURL=useModal.js.map