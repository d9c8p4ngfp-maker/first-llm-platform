import { Dialog, DialogCloseButton } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

interface KeyRevealModalProps {
  open: boolean
  apiKey: string | null
  onClose: () => void
}

export function KeyRevealModal({ open, apiKey, onClose }: KeyRevealModalProps) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="API Token 已创建"
      footer={<DialogCloseButton onClick={onClose} label="完成" />}
    >
      <p className="mb-3 text-sm text-amber-600 dark:text-amber-400">
        请立即复制保存，关闭后将无法再次查看完整密钥。
      </p>
      <code className="block break-all rounded-md bg-[hsl(var(--muted))] p-3 text-sm">{apiKey}</code>
      <Button
        className="mt-3"
        variant="outline"
        onClick={() => apiKey && navigator.clipboard.writeText(apiKey)}
      >
        复制到剪贴板
      </Button>
    </Dialog>
  )
}