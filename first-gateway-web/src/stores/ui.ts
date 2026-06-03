import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface UiState {
  infoBarExpanded: boolean
  setInfoBarExpanded: (v: boolean) => void
  toggleInfoBar: () => void
}

export const useUiStore = create<UiState>()(
  persist(
    (set, get) => ({
      infoBarExpanded: false,
      setInfoBarExpanded: (v) => set({ infoBarExpanded: v }),
      toggleInfoBar: () => set({ infoBarExpanded: !get().infoBarExpanded }),
    }),
    { name: 'fg-ui' },
  ),
)