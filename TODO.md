# TODO — Milestone 7: Project Setup, Auth & Layout Shell

---

## 1. Project Scaffolding

### Vite + React + TypeScript
- [x] Run `npm create vite@latest frontend -- --template react-ts` from project root
- [x] Verify `frontend/` directory structure: `src/`, `public/`, `index.html`, `vite.config.ts`, `tsconfig.json`

### Install dependencies
- [x] Core: `react-router-dom`, `zustand`, `axios`
- [x] Styling: `tailwindcss @tailwindcss/vite` (Tailwind v4 Vite plugin)
- [x] Icons: `lucide-react`
- [x] Toasts: `react-hot-toast`
- [x] Date: `date-fns`

### Tailwind CSS setup
- [x] Add `@tailwindcss/vite` plugin to `vite.config.ts`
- [x] Add `@import "tailwindcss"` to `src/index.css`
- [x] Verify Tailwind classes render correctly

### Vite dev proxy
- [x] In `vite.config.ts`, configure proxy: `/api` → `http://localhost:8080`, `/ws` → `http://localhost:8080` (WebSocket upgrade)

### .gitignore
- [x] Add `frontend/node_modules/`, `frontend/dist/` to project `.gitignore`

---

## 2. Project Structure (`frontend/src/`)

### Directory layout
- [x] Create directory structure:
  ```
  src/
  ├── api/           # Axios instance + API functions
  ├── components/    # Reusable UI components
  │   ├── auth/      # Login, Register, ForgotPassword modals
  │   ├── layout/    # TopNav, Sidebar, RightPanel, MainLayout
  │   └── ui/        # Button, Modal, Input, etc.
  ├── pages/         # Route-level components
  ├── stores/        # Zustand stores
  ├── types/         # TypeScript interfaces
  ├── hooks/         # Custom React hooks
  └── utils/         # Helper functions
  ```

---

## 3. TypeScript Types (`src/types/`)

### `api.ts`
- [x] `User`: `id`, `username`, `displayName`, `createdAt`
- [x] `AuthResponse`: `token`, `user: User`
- [x] `Room`: `id`, `name`, `description`, `visibility`, `owner: User`, `memberCount`, `createdAt`
- [x] `Message`: `id`, `chatType`, `sender: User`, `content`, `replyTo: Message | null`, `attachments: Attachment[]`, `editedAt`, `createdAt`
- [x] `Attachment`: `id`, `originalFilename`, `fileSize`, `mimeType`, `comment`, `downloadUrl`
- [x] `Session`: `id`, `browser`, `ipAddress`, `lastSeenAt`, `current`
- [x] `Contact`: `id`, `user: User`, `status`, `message`, `createdAt`
- [x] `MemberResponse`: `user: User`, `role`, `joinedAt`
- [x] `UnreadCount`: `roomId`, `chatUserId`, `count`

---

## 4. API Layer (`src/api/`)

### Axios instance (`src/api/client.ts`)
- [x] Create Axios instance with `baseURL: '/api'`
- [x] Request interceptor: attach `Authorization: Bearer <token>` from `useAuthStore`
- [x] Response interceptor: on 401, clear token + redirect to `/`

### Auth API (`src/api/auth.ts`)
- [x] `register(email, password, username)` → `POST /auth/register` → `AuthResponse`
- [x] `login(email, password)` → `POST /auth/login` → `AuthResponse`
- [x] `logout()` → `POST /auth/logout`
- [x] `requestPasswordReset(email)` → `POST /auth/password-reset/request`
- [x] `confirmPasswordReset(token, newPassword)` → `POST /auth/password-reset/confirm`

### User API (`src/api/users.ts`)
- [x] `getMe()` → `GET /users/me` → `User`
- [x] `updateProfile(displayName)` → `PUT /users/me` → `User`

---

## 5. Zustand Stores (`src/stores/`)

### `useAuthStore.ts`
- [x] State: `token: string | null`, `user: User | null`, `isAuthenticated: boolean`
- [x] Actions: `setAuth(token, user)`, `clearAuth()`, `loadFromStorage()`
- [x] Persist token in `localStorage`
- [x] On init: check `localStorage` for existing token → set `isAuthenticated`

### `useRoomStore.ts` (scaffold only)
- [x] State: `rooms: Room[]`, `selectedRoomId: number | null`
- [x] Actions: `setRooms(rooms)`, `selectRoom(id)` — placeholders for M8

### `useMessageStore.ts` (scaffold only)
- [x] State: `messages: Message[]`, `loading: boolean`
- [x] Actions: `setMessages(msgs)`, `addMessage(msg)` — placeholders for M8

---

## 6. Routing (`src/App.tsx`)

### React Router setup
- [x] Routes:
  - `/` → `LandingPage`
  - `/chat` → `ChatPage` (protected)
  - `*` → redirect to `/`
- [x] `ProtectedRoute` wrapper component: if `!isAuthenticated` → redirect to `/`

---

## 7. Landing Page (`src/pages/LandingPage.tsx`)

- [x] Centered layout with app logo/name "Chitchat"
- [x] Two buttons: "Sign In" and "Register"
- [x] Clicking either opens the respective auth modal
- [x] If already authenticated (token in localStorage) → redirect to `/chat`

---

## 8. Auth Modals (`src/components/auth/`)

### Reusable Modal component (`src/components/ui/Modal.tsx`)
- [x] Overlay backdrop (semi-transparent dark)
- [x] Centered white card with close button
- [x] Props: `isOpen`, `onClose`, `title`, `children`
- [x] Close on backdrop click and Escape key

### LoginModal (`src/components/auth/LoginModal.tsx`)
- [x] Fields: email (text input), password (password input)
- [x] "Sign In" button → calls `auth.login()` → on success: `setAuth(token, user)`, redirect to `/chat`
- [x] "Forgot password?" link → switches to `ForgotPasswordModal`
- [x] Inline error display on failed login
- [x] Loading state on submit button

### RegisterModal (`src/components/auth/RegisterModal.tsx`)
- [x] Fields: email, username, password, confirm password
- [x] Client-side validation: all fields required, passwords must match
- [x] "Create Account" button → calls `auth.register()` → on success: `setAuth(token, user)`, redirect to `/chat`
- [x] Inline error display on failed registration (duplicate email/username)

### ForgotPasswordModal (`src/components/auth/ForgotPasswordModal.tsx`)
- [x] Field: email
- [x] "Send Reset Link" button → calls `auth.requestPasswordReset()`
- [x] Success message: "Check your email for a reset link"

---

## 9. Chat Layout Shell (`src/pages/ChatPage.tsx` + `src/components/layout/`)

### TopNav (`src/components/layout/TopNav.tsx`)
- [x] Fixed top bar with:
  - Logo/app name (left)
  - Navigation items: "Public Rooms", "Private Rooms", "Contacts", "Sessions" (center/left)
  - User dropdown (right): display name, "Profile", "Sign out"
- [x] "Sign out" → calls `auth.logout()` → `clearAuth()` → redirect to `/`
- [x] Styled with Tailwind: dark background, white text, hover states

### Sidebar (`src/components/layout/Sidebar.tsx`)
- [x] Left column (~250px width)
- [x] Placeholder content: "Rooms will appear here" / "Contacts will appear here"
- [x] "Create Room" button at bottom (placeholder, non-functional in M7)

### ChatArea (`src/components/layout/ChatArea.tsx`)
- [x] Center column (flex-grow)
- [x] Default state: centered welcome message "Select a room or contact to start chatting"
- [x] Placeholder for message list and input bar

### RightPanel (`src/components/layout/RightPanel.tsx`)
- [x] Right column (~280px width)
- [x] Placeholder content: "Room info and members will appear here"

### ChatPage layout
- [x] Composes: `TopNav` (fixed top) + 3-column grid (`Sidebar` | `ChatArea` | `RightPanel`)
- [x] Full viewport height (`h-screen`), no page scroll
- [x] Columns use `flex` layout: sidebar fixed width, chat area grows, right panel fixed width

---

## 10. Input Components (`src/components/ui/`)

### Button (`src/components/ui/Button.tsx`)
- [x] Props: `variant` (`primary`, `secondary`, `danger`, `ghost`), `size` (`sm`, `md`, `lg`), `loading`, `disabled`, `children`, `onClick`
- [x] Tailwind-styled with hover/focus states

### Input (`src/components/ui/Input.tsx`)
- [x] Props: `label`, `type`, `placeholder`, `value`, `onChange`, `error`
- [x] Label above, error message below in red
- [x] Tailwind-styled with focus ring

---

## 11. Toast Notifications

- [x] Add `<Toaster />` from `react-hot-toast` in `App.tsx`
- [x] Use `toast.success()` / `toast.error()` for auth feedback (login success, registration error, etc.)

---

## 12. Verification

- [x] `cd frontend && npm install` — installs all dependencies
- [x] `npm run dev` — Vite dev server starts at `http://localhost:5173`
- [x] Landing page renders with Sign In / Register buttons
- [x] Sign In modal opens, submits login request to backend, receives JWT, redirects to `/chat`
- [x] Register modal opens, creates account, auto-login, redirects to `/chat`
- [x] Chat page shows 3-column layout shell with top nav
- [x] Sign out clears token, redirects to landing page
- [x] Refreshing `/chat` with valid token stays on chat page
- [x] Refreshing `/chat` without token redirects to landing
- [x] `npm run build` — production build succeeds
