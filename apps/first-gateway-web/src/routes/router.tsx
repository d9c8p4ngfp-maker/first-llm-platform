import {
  createRootRoute,
  createRoute,
  createRouter,
  redirect,
  Outlet,
} from '@tanstack/react-router'
import { AppLayout } from '@/components/layout/AppLayout'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { ChatPage } from '@/pages/chat/ChatPage'
import { ManageLayout } from '@/pages/manage/ManageLayout'
import { ChannelsPage } from '@/pages/manage/ChannelsPage'
import { TokensPage } from '@/pages/manage/TokensPage'
import { LogsPage } from '@/pages/manage/LogsPage'
import { StatsPage } from '@/pages/manage/StatsPage'
import { ModelsPage } from '@/pages/manage/ModelsPage'
import { ToolsLayout } from '@/pages/tools/ToolsLayout'
import { KnowledgePage } from '@/pages/tools/KnowledgePage'
import { KnowledgeDetailPage } from '@/pages/tools/KnowledgeDetailPage'
import { PromptsPage } from '@/pages/tools/PromptsPage'
import { SkillsPage } from '@/pages/tools/SkillsPage'
import { McpPage } from '@/pages/tools/McpPage'
import { ProfilePage } from '@/pages/profile/ProfilePage'
import { SettingsPage } from '@/pages/settings/SettingsPage'
import { PipelinePage } from '@/pages/settings/PipelinePage'
import { TOKEN_KEY } from '@/lib/utils'

function requireAuth() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) {
    throw redirect({ to: '/login' })
  }
}

const rootRoute = createRootRoute({
  component: () => <Outlet />,
})

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: LoginPage,
  beforeLoad: () => {
    if (localStorage.getItem(TOKEN_KEY)) {
      throw redirect({ to: '/chat' })
    }
  },
})

const registerRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/register',
  component: RegisterPage,
  beforeLoad: () => {
    if (localStorage.getItem(TOKEN_KEY)) {
      throw redirect({ to: '/chat' })
    }
  },
})

const appRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'app',
  component: AppLayout,
  beforeLoad: requireAuth,
})

const chatRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/chat',
  component: ChatPage,
})

const manageRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/manage',
  component: ManageLayout,
})

const manageIndexRoute = createRoute({
  getParentRoute: () => manageRoute,
  path: '/',
  beforeLoad: () => {
    throw redirect({ to: '/manage/channels' })
  },
})

const channelsRoute = createRoute({
  getParentRoute: () => manageRoute,
  path: 'channels',
  component: ChannelsPage,
})

const tokensRoute = createRoute({
  getParentRoute: () => manageRoute,
  path: 'tokens',
  component: TokensPage,
})

const logsRoute = createRoute({
  getParentRoute: () => manageRoute,
  path: 'logs',
  component: LogsPage,
})

const statsRoute = createRoute({
  getParentRoute: () => manageRoute,
  path: 'stats',
  component: StatsPage,
})

const modelsRoute = createRoute({
  getParentRoute: () => manageRoute,
  path: 'models',
  component: ModelsPage,
})

const toolsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/tools',
  component: ToolsLayout,
})

const toolsIndexRoute = createRoute({
  getParentRoute: () => toolsRoute,
  path: '/',
  beforeLoad: () => {
    throw redirect({ to: '/tools/knowledge' })
  },
})

const knowledgeRoute = createRoute({
  getParentRoute: () => toolsRoute,
  path: 'knowledge',
  component: KnowledgePage,
})

const knowledgeDetailRoute = createRoute({
  getParentRoute: () => toolsRoute,
  path: 'knowledge/$kbId',
  component: KnowledgeDetailPage,
})

const promptsRoute = createRoute({
  getParentRoute: () => toolsRoute,
  path: 'prompts',
  component: PromptsPage,
})

const skillsRoute = createRoute({
  getParentRoute: () => toolsRoute,
  path: 'skills',
  component: SkillsPage,
})

const mcpRoute = createRoute({
  getParentRoute: () => toolsRoute,
  path: 'mcp',
  component: McpPage,
})

const profileRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/profile',
  component: ProfilePage,
})

const settingsRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/settings',
  component: SettingsPage,
})

const pipelineRoute = createRoute({
  getParentRoute: () => appRoute,
  path: '/settings/pipeline',
  component: PipelinePage,
})

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  beforeLoad: () => {
    throw redirect({ to: localStorage.getItem(TOKEN_KEY) ? '/chat' : '/login' })
  },
})

const routeTree = rootRoute.addChildren([
  indexRoute,
  loginRoute,
  registerRoute,
  appRoute.addChildren([
    chatRoute,
    manageRoute.addChildren([
      manageIndexRoute,
      channelsRoute,
      tokensRoute,
      logsRoute,
      statsRoute,
      modelsRoute,
    ]),
    toolsRoute.addChildren([
      toolsIndexRoute,
      knowledgeRoute,
      knowledgeDetailRoute,
      promptsRoute,
      skillsRoute,
      mcpRoute,
    ]),
    profileRoute,
    settingsRoute,
    pipelineRoute,
  ]),
])

export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}