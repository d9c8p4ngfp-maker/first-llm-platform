import { useEffect, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { ChevronRight } from "lucide-react"
import { useThemeStore, type ThemeMode } from "@/stores/theme"
import { cn } from "@/lib/utils"
import { settingsApi } from "@/api/settings"
import { modelsApi } from "@/api/workspace"
import { profileApi, type ProfileStatus } from "@/api/profile"

const themeLabels: Record<ThemeMode, string> = {
  light: "浅色",
  dark: "深色",
  system: "跟随系统",
}

const aiFeatures: { key: keyof ProfileStatus; title: string }[] = [
  { key: "memoryEnabled",  title: "记忆学习" },
  { key: "profileEnabled", title: "用户画像" },
  { key: "profileInChat",  title: "个性化回复" },
]

export function SettingsPage() {
  const qc = useQueryClient()
  const mode = useThemeStore((s) => s.mode)
  const setMode = useThemeStore((s) => s.setMode)
  const { data: settings } = useQuery({ queryKey: ["settings"], queryFn: settingsApi.get })
  const { data: models = [] } = useQuery({ queryKey: ["models"], queryFn: modelsApi.list })
  const { data: profileStatus } = useQuery({ queryKey: ["profile-status"], queryFn: profileApi.getStatus })
  const [defaultModel, setDefaultModel] = useState("")
  const [localStatus, setLocalStatus] = useState<ProfileStatus | null>(null)
  const [showModelPicker, setShowModelPicker] = useState(false)
  const [showThemePicker, setShowThemePicker] = useState(false)

  useEffect(() => {
    if (settings?.default_model) setDefaultModel(settings.default_model)
  }, [settings])

  useEffect(() => {
    if (profileStatus) setLocalStatus(profileStatus)
  }, [profileStatus])

  const saveSettingsMut = useMutation({
    mutationFn: () => settingsApi.update({ defaultModel, theme: mode }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["settings"] })
      qc.invalidateQueries({ queryKey: ["model-prefs"] })
    },
  })

  const saveProfileMut = useMutation({
    mutationFn: profileApi.updateStatus,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["profile-status"] }),
  })

  function setModel(name: string) {
    setDefaultModel(name)
    setShowModelPicker(false)
    settingsApi.update({ defaultModel: name, theme: mode }).then(() => {
      qc.invalidateQueries({ queryKey: ["settings"] })
      qc.invalidateQueries({ queryKey: ["model-prefs"] })
    })
  }

  function pickTheme(m: ThemeMode) {
    setMode(m)
    setShowThemePicker(false)
    saveSettingsMut.mutate()
  }

  function toggle(key: keyof ProfileStatus) {
    if (!localStatus) return
    const next = { ...localStatus, [key]: !localStatus[key] }
    setLocalStatus(next)
    saveProfileMut.mutate(next)
  }

  return (
    <div className="mx-auto max-w-lg space-y-8 pb-8">
      {/* iOS-style nav title */}
      <div className="pt-2">
        <h1 className="text-[28px] font-bold tracking-tight text-[hsl(var(--foreground))]">
          设置
        </h1>
      </div>

      {/* === 外观 === */}
      <section>
        <h2 className="mb-2 ml-4 text-[13px] font-medium uppercase tracking-wide text-[hsl(var(--muted-foreground))]">
          外观
        </h2>
        <div className="overflow-hidden rounded-xl bg-[hsl(var(--card))] shadow-sm">
          <button
            onClick={() => setShowThemePicker(true)}
            className="flex w-full items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] active:bg-black/[0.04] transition-colors"
          >
            <span className="text-[15px] text-[hsl(var(--foreground))]">主题</span>
            <div className="flex items-center gap-1 text-[15px] text-[hsl(var(--muted-foreground))]">
              <span>{themeLabels[mode]}</span>
              <ChevronRight className="h-4 w-4" />
            </div>
          </button>
        </div>
      </section>

      {/* === 默认模型 === */}
      <section>
        <h2 className="mb-2 ml-4 text-[13px] font-medium uppercase tracking-wide text-[hsl(var(--muted-foreground))]">
          对话
        </h2>
        <div className="overflow-hidden rounded-xl bg-[hsl(var(--card))] shadow-sm">
          <button
            onClick={() => setShowModelPicker(true)}
            className="flex w-full items-center justify-between px-4 py-3 text-left hover:bg-black/[0.02] active:bg-black/[0.04] transition-colors"
          >
            <span className="text-[15px] text-[hsl(var(--foreground))]">默认模型</span>
            <div className="flex items-center gap-1 text-[15px] text-[hsl(var(--muted-foreground))]">
              <span>{defaultModel || "未选择"}</span>
              <ChevronRight className="h-4 w-4" />
            </div>
          </button>
        </div>
      </section>

      {/* === AI 个性化 === */}
      <section>
        <h2 className="mb-2 ml-4 text-[13px] font-medium uppercase tracking-wide text-[hsl(var(--muted-foreground))]">
          AI 个性化
        </h2>
        <div className="overflow-hidden rounded-xl bg-[hsl(var(--card))] shadow-sm">
          {localStatus && aiFeatures.map((feat, i) => {
            const active = localStatus[feat.key]
            return (
              <div
                key={feat.key}
                className={cn(
                  "flex items-center justify-between px-4 py-3",
                  i < aiFeatures.length - 1 && "border-b border-[hsl(var(--border))]"
                )}
              >
                <span className="text-[15px] text-[hsl(var(--foreground))]">{feat.title}</span>
                <button
                  type="button"
                  role="switch"
                  aria-checked={active}
                  onClick={() => toggle(feat.key)}
                  className={cn(
                    "relative inline-flex h-[31px] w-[51px] shrink-0 cursor-pointer rounded-full transition-colors duration-200",
                    active ? "bg-brand" : "bg-[hsl(var(--border))]"
                  )}
                >
                  <span
                    className={cn(
                      "absolute top-[2px] h-[27px] w-[27px] rounded-full bg-white shadow-md transition-all duration-200",
                      active ? "left-[22px]" : "left-[2px]"
                    )}
                  />
                </button>
              </div>
            )
          })}
        </div>
        <p className="mt-2 ml-4 text-[12px] text-[hsl(var(--muted-foreground))] leading-relaxed">
          开启后，AI 将从对话中学习你的偏好，并提供更个性化的回复体验。
        </p>
      </section>

      {/* === 关于 === */}
      <section>
        <h2 className="mb-2 ml-4 text-[13px] font-medium uppercase tracking-wide text-[hsl(var(--muted-foreground))]">
          关于
        </h2>
        <div className="overflow-hidden rounded-xl bg-[hsl(var(--card))] shadow-sm">
          <div className="flex items-center justify-between px-4 py-3">
            <span className="text-[15px] text-[hsl(var(--foreground))]">版本</span>
            <span className="text-[15px] text-[hsl(var(--muted-foreground))]">1.0.0</span>
          </div>
        </div>
      </section>

      {/* === Theme Picker Modal === */}
      {showThemePicker && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/30" onClick={() => setShowThemePicker(false)}>
          <div
            className="w-full max-w-lg animate-slide-up rounded-t-2xl bg-[hsl(var(--card))] pb-8 pt-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-[hsl(var(--border))]" />
            <h3 className="px-6 pb-3 text-center text-[15px] font-semibold text-[hsl(var(--foreground))]">选择主题</h3>
            <div className="mx-4 overflow-hidden rounded-xl">
              {(["light", "dark", "system"] as ThemeMode[]).map((m, i) => (
                <button
                  key={m}
                  onClick={() => pickTheme(m)}
                  className={cn(
                    "flex w-full items-center justify-between px-4 py-3 text-left text-[15px] transition-colors hover:bg-black/[0.02] active:bg-black/[0.04]",
                    i < 2 && "border-b border-[hsl(var(--border))]"
                  )}
                >
                  <span className="text-[hsl(var(--foreground))]">{themeLabels[m]}</span>
                  {mode === m && (
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M5 10l3.5 3.5L15 7" stroke="hsl(150,50%,42%)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* === Model Picker Modal === */}
      {showModelPicker && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/30" onClick={() => setShowModelPicker(false)}>
          <div
            className="w-full max-w-lg animate-slide-up rounded-t-2xl bg-[hsl(var(--card))] pb-8 pt-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-[hsl(var(--border))]" />
            <h3 className="px-6 pb-3 text-center text-[15px] font-semibold text-[hsl(var(--foreground))]">选择默认模型</h3>
            <div className="mx-4 overflow-hidden rounded-xl">
              {models.map((m, i) => (
                <button
                  key={m.id}
                  onClick={() => setModel(m.name)}
                  className={cn(
                    "flex w-full items-center justify-between px-4 py-3 text-left text-[15px] transition-colors hover:bg-black/[0.02] active:bg-black/[0.04]",
                    i < models.length - 1 && "border-b border-[hsl(var(--border))]"
                  )}
                >
                  <span className="text-[hsl(var(--foreground))]">{m.name}</span>
                  {defaultModel === m.name && (
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                      <path d="M5 10l3.5 3.5L15 7" stroke="hsl(150,50%,42%)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                    </svg>
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* slide-up animation */}
      <style>{`
        @keyframes slide-up {
          from { transform: translateY(100%); }
          to { transform: translateY(0); }
        }
        .animate-slide-up {
          animation: slide-up 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }
      `}</style>
    </div>
  )
}
