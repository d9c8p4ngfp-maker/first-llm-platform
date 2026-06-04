import { useEffect, useState } from "react"
import { Link } from "@tanstack/react-router"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Settings } from "lucide-react"
import { PageHeader } from "@/components/shared/PageHeader"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useThemeStore, type ThemeMode } from "@/stores/theme"
import { cn } from "@/lib/utils"
import { settingsApi } from "@/api/settings"
import { modelsApi } from "@/api/workspace"

const modes: { id: ThemeMode; label: string }[] = [
  { id: "light", label: "浅色" },
  { id: "dark", label: "深色" },
  { id: "system", label: "跟随系统" },
]

export function SettingsPage() {
  const qc = useQueryClient()
  const mode = useThemeStore((s) => s.mode)
  const setMode = useThemeStore((s) => s.setMode)
  const { data: settings } = useQuery({ queryKey: ["settings"], queryFn: settingsApi.get })
  const { data: models = [] } = useQuery({ queryKey: ["models"], queryFn: modelsApi.list })
  const [defaultModel, setDefaultModel] = useState("")

  useEffect(() => {
    if (settings?.default_model) setDefaultModel(settings.default_model)
  }, [settings])

  const saveMut = useMutation({
    mutationFn: () => settingsApi.update({ defaultModel, theme: mode }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["settings"] })
      qc.invalidateQueries({ queryKey: ["model-prefs"] })
    },
  })

  return (
    <div className="mx-auto max-w-lg space-y-6">
      <PageHeader title="设置" description="主题与默认模型偏好" />
      <div className="space-y-3 rounded-xl border border-[hsl(var(--border))] p-4">
        <Label htmlFor="theme-mode">主题模式</Label>
        <div id="theme-mode" className="flex gap-2">
          {modes.map((m) => (
            <button
              key={m.id}
              type="button"
              onClick={() => setMode(m.id)}
              className={cn(
                "rounded-md border px-3 py-2 text-sm",
                mode === m.id
                  ? "border-[hsl(var(--foreground))] bg-[hsl(var(--muted))]"
                  : "border-[hsl(var(--border))]",
              )}
            >
              {m.label}
            </button>
          ))}
        </div>
      </div>
      <div className="space-y-3 rounded-xl border border-[hsl(var(--border))] p-4">
        <Label htmlFor="default-model">默认模型（同步服务器）</Label>
        <Select value={defaultModel} onValueChange={setDefaultModel}>
          <SelectTrigger id="default-model">
            <SelectValue placeholder="选择模型" />
          </SelectTrigger>
          <SelectContent>
            {models.map((m) => (
              <SelectItem key={m.id} value={m.name}>
                {m.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button onClick={() => saveMut.mutate()} disabled={saveMut.isPending}>
          保存设置
        </Button>
      </div>
      <div className="space-y-3 rounded-xl border border-[hsl(var(--border))] p-4">
        <Label>高级设置</Label>
        <Link
          to="/settings/pipeline"
          className="flex items-center gap-2 rounded-md border border-[hsl(var(--border))] px-3 py-2 text-sm text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))] transition-colors"
        >
          <Settings className="h-4 w-4" />
          Pipeline 配置
        </Link>
      </div>
    </div>
  )
}
